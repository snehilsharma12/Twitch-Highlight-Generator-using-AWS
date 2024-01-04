package com.twitch.bot.scheduler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobResult;
import com.twitch.bot.api.ApiHandler;
import com.twitch.bot.api.ApiHandler.PATH;
import com.twitch.bot.aws_technologies.AWS_Comprehend;
import com.twitch.bot.aws_technologies.AWS_Credentials;
import com.twitch.bot.aws_technologies.AWS_S3;
import com.twitch.bot.aws_technologies.AWS_Sns;
import com.twitch.bot.aws_technologies.AWS_Transcribe;
import com.twitch.bot.db_utils.TwitchAWS_RDS;
import com.twitch.bot.db_utils.TwitchData;
import com.twitch.bot.dynamo_db_model.MessagesCount;
import com.twitch.bot.dynamo_db_model.TwitchAnalysis;
import com.twitch.bot.dynamo_db_model.TwitchAnalysis.ClipsDetails;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.Subscriptions;
import com.twitch.bot.sns_model.SnsData;
import com.twitch.bot.twitch_connection.ChannelsData;

@Lazy(false)
@Component
public class ScheduleTwitchLogic {
    private static final Logger LOG = Logger.getLogger(ScheduleTwitchLogic.class.getName());
    private static final long frequencySeconds = 15000l;
    private Long coolDownMillis = 0l;
    private Long offsetMillis= 0l;
    private TwitchData twitchData;
    private ApiHandler apiHandler;
    private TwitchAWS_RDS rdsConnection;
    private String awsTranscribeBucketName = "twitch-transcribe-bucket-" + System.getenv("AWS_ACCOUNT_ID");

    public ScheduleTwitchLogic(TwitchData twitchData, ApiHandler apiHandler, TwitchAWS_RDS twitchAWS_RDS, @Value("${twitch.analysis.cooldown.seconds}") Long coolDownSeconds, @Value("${twitch.analysis.start.offset.minutes}") Long offsetMinutes){
        this.twitchData = twitchData;
        this.apiHandler = apiHandler;
        this.coolDownMillis = coolDownSeconds * 1000;
        this.offsetMillis = offsetMinutes * 60 * 1000;
        this.rdsConnection = twitchAWS_RDS;
    }
    @Scheduled(fixedRate = 15000)
    public void jobRunner() throws Exception {
        Long currentTime = System.currentTimeMillis();
        LOG.log(Level.INFO, "currentTime In Schedule ::: " + currentTime);

        String channelTiming = "";

        List<Channel> allChannelNames = getChannelNames();
        Iterator<Channel> allChannelNamesIter = allChannelNames.iterator();
        while(allChannelNamesIter.hasNext()){
            Channel channel = allChannelNamesIter.next();
            LOG.log(Level.INFO, "Channel Name - {0}", new Object[]{channel.getChannelName()});
            Long startTime = System.currentTimeMillis();
            if(channel.getIsListeningToChannel()){
                processChannelMessages(channel, currentTime);
            }else{
                twitchData.deleteTwitchMessageForChannel(channel, currentTime);
            }
            channelTiming += (channelTiming.trim() == "") ? channel.getChannelName() + " - " + (System.currentTimeMillis() - startTime) : ", " + channel.getChannelName() + " - " + (System.currentTimeMillis() - startTime);
        }
        LOG.log(Level.INFO, "Scheduler Run Time ::: " + channelTiming);
    }

    public List<Channel> getChannelNames() {
        HashMap<String, Channel> data = ChannelsData.getChannels();
        if (null == data || data.size() == 0) {
            return twitchData.getChannelDetails();
        }else{
            return new ArrayList<>(data.values());
        }
    }

    public void processChannelMessages(Channel channel, Long tillTimeStamp) throws Exception {
        JSONObject channelDtls = getChannelDetails(channel);
        if (channelDtls.getBoolean("is_channel_live")) {

            Long startedAt = Long.valueOf(channelDtls.get("stream_started_at").toString());
            JSONArray messages = twitchData.getTwitchMessageForChannel(channel, tillTimeStamp - frequencySeconds,
                    tillTimeStamp);
            twitchData.addMessageCountBasedOnRollingWindow(channel, Long.valueOf(messages.length()), tillTimeStamp);
            Long thresholdValue = getThresholdValueBasedOnChannel(channel);
            if(thresholdValue == -1){
                LOG.log(Level.INFO, "Rolling Window Data not populated for channel {0}", new Object[]{channel.getChannelName()});
            }
            else if(thresholdValue == 0){
                LOG.log(Level.INFO, "No Messages for channel {0}", new Object[]{channel.getChannelName()});
            }
            else if ((startedAt + offsetMillis) >= tillTimeStamp) {
                LOG.log(Level.INFO, "Channel {0} Start Time {1} is under offsetValue {2} for timestamp {3}",
                        new Object[] { channel.getChannelName(), startedAt, offsetMillis, tillTimeStamp });
            }else if (messages.length() >= thresholdValue) {
                List<TwitchAnalysis> twitchAnalysis = twitchData.getTwitchAnalysisRawDataOfAChannel(channel,
                        false);
                if (!twitchAnalysis.isEmpty()
                        && (twitchAnalysis.get(0).getTimestamp() + coolDownMillis) > tillTimeStamp) {
                    LOG.log(Level.INFO,
                            "Last Generated Data Time is {0} which is not exceeds the current cooldown of {1} seconds from current time {2}",
                            new Object[] { twitchAnalysis.get(0).getTimestamp(), coolDownMillis, tillTimeStamp });
                    twitchData.deleteTwitchMessageForChannel(channel, tillTimeStamp);
                    return;
                }
                ClipsDetails clips = awsClipsGeneration(channel);
                JSONObject credentials = twitchData.getCloudCredentials();  
                AWS_Credentials awsCredentials = new AWS_Credentials(credentials.get("access_key").toString(), credentials.get("access_id").toString());
                if(clips != null){
                    LOG.log(Level.INFO,"clips ::: " + clips);
                    AWS_Comprehend comprehend = new AWS_Comprehend(messageMerge(messages), awsCredentials);
                    String sentimental_result = comprehend.getSentiment();
                    LOG.log(Level.INFO,"sentimental_result ::: " + sentimental_result);
                    
                    String videoUrl = clips.getThumbnail_url().substring(0,  clips.getThumbnail_url().lastIndexOf("preview") - 1);
                    videoUrl += ".mp4";
                    String transcribedData = null;
                    
                    try{
                        transcribedData = awsTranscribeConversion(videoUrl, channel, awsCredentials);
                        LOG.log(Level.INFO, "TranscribedData ::: " + transcribedData);
                    }catch(Exception ex){
                        LOG.log(Level.SEVERE, "Exception in Transcribe ::: " + ex.getMessage());
                    }

                     String video_sentimental_result = "NEUTRAL";
                    if(transcribedData != null){
                        String message = getTranscribedDataMessage(transcribedData);
                        if(message != null){
                            comprehend =  new AWS_Comprehend(message, awsCredentials);
                            video_sentimental_result = comprehend.getSentiment();
                        }
                    }
                   
                    twitchData.addTwitchAnalysis(channel, sentimental_result, video_sentimental_result, clips, System.currentTimeMillis());
                    
                    
                    AWS_Sns sns = new AWS_Sns(awsCredentials);
                    SnsData data = new SnsData();
                    data.setUserId(getSubscribedUserIds(channel));
                    data.setChannelId(channel.getId());
                    data.setChannelName(channel.getChannelName());
                    sns.publishSNSMessage(data);
                }              
            }
            twitchData.deleteTwitchMessageForChannel(channel, tillTimeStamp);
        } else {
            twitchData.clearMessagesCountForAChannel(channel);
        }
    }

    private List<Integer> getSubscribedUserIds(Channel channel){
        List<Integer> userIds = new ArrayList<>();
        try{
            List<Subscriptions> data = rdsConnection.getSubscriptionDetailsBasedOnUserOrSubscriptionId(channel.getId(), false);
            Iterator<Subscriptions> dataIter = data.iterator();
            while(dataIter.hasNext()){
                Subscriptions subs = dataIter.next();
                userIds.add(subs.getUserId());
            }
        }catch(Exception ex){
            LOG.log(Level.SEVERE, "Exception in fetching userIds ::: " + ex.getMessage());
        }
       return userIds;
    }

    private String getTranscribedDataMessage(String transcribedData){
        try{
            JSONObject data = new JSONObject(transcribedData);
            JSONArray transcripts = data.getJSONObject("results").getJSONArray("transcripts");
            Iterator<Object> transcriptsIter = transcripts.iterator();
            String message = "";
            while(transcriptsIter.hasNext()){
                JSONObject value = (JSONObject)transcriptsIter.next();
                if(message != ""){
                    message += " ";
                }
                message += value.get("transcript");
            }
            return message;
        }catch(Exception ex){
            LOG.log(Level.SEVERE, "Exception in Transcribe Data Manipulation ::: " + ex.getMessage());
        }
        return null;
    }

    public JSONObject getChannelDetails(Channel channel) throws Exception{
        String response = apiHandler.setPath(PATH.GET_STREAMS).setParams(new JSONObject().put("user_login", channel.getChannelName())).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).GET();
        JSONObject responseData = new JSONObject(response);
        JSONObject channelDtls = new JSONObject();
        if(responseData.isEmpty()){
            channelDtls.put("is_channel_live", false);
        }else if(responseData.getJSONArray("data").isEmpty()){
            channelDtls.put("is_channel_live", false);
        }else{
            JSONObject data = responseData.getJSONArray("data").getJSONObject(0);
            Boolean isChannelLive = data.get("type").toString().equalsIgnoreCase("live");
            channelDtls.put("is_channel_live", isChannelLive);
            if(isChannelLive){
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = inputFormat.parse(data.get("started_at").toString());
                channelDtls.put("stream_started_at", date.getTime());
            }   
        }
        return channelDtls;
    }

    public Long getThresholdValueBasedOnChannel(Channel channel) throws Exception {
        // if(TwitchData.isAwsEnvironment() && channel.getChannelName().equals("shroud")){
        //     return 1l;
        // }
        List<MessagesCount> msgCountData = twitchData.getMessageCountDataOfAChannel(channel);
        Long thresholdValue = -1l;
        if (!msgCountData.isEmpty()) {
            thresholdValue = 0l;
            for (MessagesCount msgData : msgCountData) {
                thresholdValue += msgData.getMessageCount();
            }
            thresholdValue = thresholdValue / (msgCountData.size() * 4);
        }
        return thresholdValue;
    }


    public String awsTranscribeConversion(String videoUrl, Channel channel, AWS_Credentials awsCredentials) throws Exception{
        AWS_S3 s3 = new AWS_S3(awsTranscribeBucketName, awsCredentials);
        if (!s3.isAwsTranscribeBucketExists()) {
            s3.createAwsTranscribeBucket();
        }

        String key = channel.getTwitchId() + "_" + channel.getChannelName() + "_" + channel.getId();

        String s3Key = key + "." + "mp4";
        s3.uploadFileToAwsBucket(s3Key, downloadUrl(new URL(videoUrl)));

        String transcribeJobName = key + "_" + "transcribe";

        AWS_Transcribe transcribe = new AWS_Transcribe(transcribeJobName, s3Key, s3, awsCredentials, Regions.US_EAST_1);
        transcribe.initateTranscribe();
        GetTranscriptionJobResult getTranscriptionResult = null;
        String response = null;
        try{
            getTranscriptionResult = transcribe.getTranscriptionJobResult();
        }catch(Exception ex){
            if(ex.getMessage() != null && ex.getMessage().equals("NO_AUDIO")){
                response = "NO AUDIO";
            }else{
                LOG.log(Level.SEVERE, "Exception in extracting audio ::: " + ex.getMessage());
                response = "EXCEPTION_IN_TRANSCRIPT";
                getTranscriptionResult = null;
            }
        }
        if(getTranscriptionResult != null){
            String transcriptFileUriString = getTranscriptionResult.getTranscriptionJob().getTranscript()
            .getTranscriptFileUri();
            response = transcribe.downloadTranscriptionResponse(transcriptFileUriString);
        }
        s3.deleteFileFromAwsBucket(s3Key);
        transcribe.deleteTranscriptionJob();
        return response;
    }

    private InputStream downloadUrl(URL toDownload) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InputStream stream = null;
        try {
            byte[] chunk = new byte[4096];
            int bytesRead;
            stream = toDownload.openStream();
    
            // while ((bytesRead = stream.read(chunk)) > 0) {
            //     outputStream.write(chunk, 0, bytesRead);
            // }
    
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex);
            return null;
        }
        return stream;
        //return ByteBuffer.wrap(outputStream.toByteArray());
    }

    public String messageMerge(JSONArray messages){
        String messagesStr = "";
        Iterator<Object> messagesIter = messages.iterator();
        while(messagesIter.hasNext()){
            JSONObject messageObj = (JSONObject)messagesIter.next();
            messagesStr += messageObj.get("message").toString();
        }
        return messagesStr;
    }

    public ClipsDetails awsClipsGeneration(Channel channel) throws Exception{
        JSONObject data = new JSONObject();
        String response = apiHandler.setPath(PATH.CLIPS).setParams(new JSONObject().put("broadcaster_id", channel.getTwitchId())).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).POST();
        JSONObject responseData = new JSONObject(response);
        LOG.log(Level.INFO,"CLIPS:::responseData in clips 1 ::: " + responseData);
        if(!responseData.has("data")){
            LOG.log(Level.SEVERE, "Clips Generation Issue for Channel ::: {0} ::: Response ::: {1}", new Object[]{channel.getChannelName(), responseData});
            return null;
        }
        String clip_id = responseData.getJSONArray("data").getJSONObject(0).getString("id");
        LOG.log(Level.INFO,"CLIPS:::clip_id in clips 1.1 ::: " + clip_id);
        Thread.sleep(5000);//*Thread Sleeps so that the create clip is done generating on twitch side */
        response = apiHandler.setPath(PATH.CLIPS).setParams(new JSONObject().put("id", clip_id)).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).GET();
        responseData = new JSONObject(response);
        LOG.log(Level.INFO,"CLIPS:::responseData in clips 2 ::: " + responseData);
        responseData = responseData.getJSONArray("data").getJSONObject(0);
        ClipsDetails clipsDetails = new ClipsDetails();
        clipsDetails.setClip_id(clip_id);
        clipsDetails.setVideo_url(responseData.get("url").toString());
        clipsDetails.setEmbed_url(responseData.get("embed_url").toString());
        clipsDetails.setThumbnail_url(responseData.get("thumbnail_url").toString());
        clipsDetails.setCreated_at(responseData.get("created_at").toString());
        data.put("clip_id", clip_id);
        data.put("video_url", responseData.get("url").toString());
        data.put("embed_url", responseData.get("embed_url").toString());
        data.put("created_at", responseData.get("created_at").toString());
        data.put("thumbnail_url", responseData.get("thumbnail_url").toString()); 
        LOG.log(Level.INFO,"CLIPS:::data in clips 3 ::: " + clipsDetails.toString());
        return clipsDetails;
    }
}
