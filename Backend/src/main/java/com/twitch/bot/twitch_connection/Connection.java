package com.twitch.bot.twitch_connection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitch.bot.api.ApiHandler;
import com.twitch.bot.api.ApiHandler.PATH;
import com.twitch.bot.db_utils.TwitchAWS_DynamoDB;
import com.twitch.bot.db_utils.TwitchData;
import com.twitch.bot.dynamo_db_model.TwitchAnalysis;
import com.twitch.bot.dynamo_db_model.TwitchAnalysis.SentimentalData;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.User;
import org.springframework.context.annotation.DependsOn;

@Component
@DependsOn({"users", "twitchData", "apiHandler"})
public class Connection {
    private static final Logger LOG = Logger.getLogger(Connection.class.getName());
    private ApiHandler apiHandler;
    private Boolean isConnectionRunning = false;
    private Boolean isStartReadingMessagesStarted = false;
    private BufferedReader twitch_reader;
    private BufferedWriter twitch_writer;
    private TwitchData twitchData;
    private Users users;

    public TwitchData getTwitchData() {
        return twitchData;
    }

    public BufferedReader getTwitch_reader() {
        return twitch_reader;
    }

    public BufferedWriter getTwitch_writer() {
        return twitch_writer;
    }

    public Connection(ApiHandler apiHandler, TwitchData twitchData, @Value("${mandatory.channel.names}") String channelNames, Users users, TwitchAWS_DynamoDB dynamo) throws Exception {
        this.apiHandler = apiHandler;
        this.twitchData = twitchData;
        this.users = users;
        this.connect();
        HashMap<String, Channel> channels = ChannelsData.getChannels();
        if(channels.isEmpty()){
            LOG.log(Level.INFO, "Channels Empty");
            String[] channelNamesArr = channelNames.split(",");
            populateMandatoryChannels(Arrays.asList(channelNamesArr));
        }else{
            Iterator<String> channelsIter = channels.keySet().iterator();
            while(channelsIter.hasNext()){
                Channel channel = channels.get(channelsIter.next());
                this.joinChannel(channel.getChannelName());
            }
        } 
        dynamo.PrePopulateDataInDB();
    }

    private void populateMandatoryChannels(List<String> channelNames) throws Exception{
        Iterator<String> channelNamesIter = channelNames.iterator();
        while(channelNamesIter.hasNext()){
            String channelName = channelNamesIter.next();
            addAndJoinChannel(channelName);
        }
    }

    public void sendCommandMessage(Object message) {
        try {
            this.twitch_writer.write(message + " \r\n");
            this.twitch_writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.log(Level.INFO, message.toString());
    }

    public void sendMessage(Object message, Channel channel) {
        try {
            this.twitch_writer.write("PRIVMSG " + "#" + channel.getChannelName() + " :" + message.toString() + "\r\n");
            this.twitch_writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.log(Level.INFO, message.toString());
    }

    public void addAndJoinChannel(String channelName) throws Exception{
        String broadcaster_id = getUserBroadcasterId(channelName);
        new ChannelsData(twitchData).addChannel(channelName, broadcaster_id);
        joinChannel(channelName);
    }

    
    public void removeAndDeleteChannelData(String channelName) throws Exception{
        Channel channel = ChannelsData.getChannel(channelName);
        if(channel != null){
            removeChannel(channel.getChannelName());
            twitchData.deleteChannelRelatedInfo(channel);
        }
    }

    public void joinChannel(String channelName){
        ChannelsData.joinChannel(channelName, this);
    }

    public void removeChannel(String channelName){
        ChannelsData.stopListeningToChannel(channelName, this);
    }

     /*
     * If the server stops unexpectly, on restart all connected channels must be cleared
     */
    public void cleanUp(){
        ChannelsData.stopListeningToChannel("tubbo", this);
    }

    public Boolean connect() throws Exception {
        //Boolean isFirstTimeConnect = !isConnectionRunning;
        if (!isConnectionRunning) {
            isConnectionRunning = apiHandler.CONNECT();
        }
        this.twitch_writer = apiHandler.getTwitch_writer();
        this.twitch_reader = apiHandler.getTwitch_reader();
        LOG.log(Level.INFO, "Log Check");
        // if(isFirstTimeConnect){
        //     cleanUp();
        // }
        startReadingMessages();
        return isConnectionRunning;
    }

    private void startReadingMessages() {
        if (isStartReadingMessagesStarted) {
            return;
        }
        isStartReadingMessagesStarted = true;
        new Thread(() -> {
            readTwitchMessage();
        }).start();
    }

    public void readTwitchMessage(){
        String currentLine = "";
        try {
            while ((currentLine = this.twitch_reader.readLine()) != null) {
                if (currentLine.toLowerCase().startsWith("ping")) {
                    processPingMessage(currentLine);
                } else if (currentLine.contains("PRIVMSG")) {
                    processMessage(currentLine);
                } else if (currentLine.toLowerCase().contains("disconnected")) {
                    //LOG.log(Level.INFO, currentLine);
                    apiHandler.CONNECT();
                } else {
                    //LOG.log(Level.INFO, currentLine);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception is " + ex);
            isStartReadingMessagesStarted= false;
        }
    }

    private void processPingMessage(String currentLine) throws Exception {
        this.twitch_writer.write("PONG " + currentLine.substring(5) + "\r\n");
        this.twitch_writer.flush();
    }

    private void processMessage(String currentLine) {
        String str[];
        str = currentLine.split("!");
        String msg_user = str[0].substring(1, str[0].length());
        str = currentLine.split(" ");
        Channel msg_channel = ChannelsData.getChannel(str[2].startsWith("#") ? str[2].substring(1) : str[2]);
        String msg_msg = currentLine.substring((str[0].length() + str[1].length() + str[2].length() + 4), currentLine.length());
        //LOG.log(Level.INFO, "Channel Details : " + msg_channel + " ||| User : " + msg_user + " ||| Messsage : " + msg_msg);
        if (msg_msg.startsWith("!")){
            processCommand(msg_user, msg_channel, msg_msg.substring(1));
        }
        // if (msg_user.toString().equals("jtv") && msg_msg.contains("now hosting")) {
        //     String hoster = msg_msg.split(" ")[0];
        //     processHost(ChannelsData.getUser(hoster), msg_channel);
        // }
        processMessage(msg_user, msg_channel, msg_msg);
    }

    private void processCommand(String user, Channel channel, String command){

    }

    private void processMessage(String user, Channel channel, String message)
	{
        twitchData.addTwitchMessage(user, channel, message, System.currentTimeMillis());
	}

    public String getUserBroadcasterId(String name) throws Exception{
        String response = apiHandler.setPath(PATH.GET_USERS).setParams(new JSONObject().put("login", name)).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).GET();
        JSONObject responseData = new JSONObject(response);
        String broadcaster_id = responseData.getJSONArray("data").getJSONObject(0).getString("id");
        LOG.log(Level.INFO, "broadcaster_id :::: " + broadcaster_id);
        return broadcaster_id;
    }

    public void makeClips(String broadcaster_id) throws Exception{
        String response = apiHandler.setPath(PATH.CLIPS).setParams(new JSONObject().put("broadcaster_id", broadcaster_id)).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).POST();
        JSONObject responseData = new JSONObject(response);
        String clip_id = responseData.getJSONArray("data").getJSONObject(0).getString("id");
        LOG.log(Level.INFO, "clip_id :::: " + clip_id);
        Thread.sleep(500);//*Thread Sleeps so that the create clip is done generating on twitch side */
        response = apiHandler.setPath(PATH.CLIPS).setParams(new JSONObject().put("id", "GoodObedientGazelleBigBrother-4fwYP4VvZEr4BcNg")).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).GET();
        LOG.log(Level.INFO, "response :::: " + response);
    }

    public JSONArray getTwitchAnalysisOfAChannelInJSON(String channelName){
        return twitchData.getTwitchAnalysisOfAChannel(ChannelsData.getChannel(channelName), true);
    }

    public List<HashMap<String,Object>> getTwitchAnalysisOfAChannelInListOfHashmap(String channelName){
        JSONArray data = twitchData.getTwitchAnalysisOfAChannel(ChannelsData.getChannel(channelName), true);
        Iterator<Object> dataIter = data.iterator();
        List<HashMap<String,Object>> result = new ArrayList<>();
        while(dataIter.hasNext()){
            SentimentalData value = (SentimentalData)dataIter.next();
            HashMap<String,Object> sentimentalData = new ObjectMapper().convertValue(value, new TypeReference<HashMap<String, Object>>(){});
            result.add(sentimentalData);
        }
        return result;
    }

    public List<TwitchAnalysis> getTwitchAnalysisOfAChannel(String channelName){
        return twitchData.getTwitchAnalysisRawDataOfAChannel(ChannelsData.getChannel(channelName), true);
    }

    public List<HashMap<String, Object>> getAllChannels(User user) throws Exception{
        HashMap<String, Channel> channels = ChannelsData.getChannels();
        LOG.log(Level.INFO, "channels ::: "+ channels);
        Iterator<String> channelsIter = channels.keySet().iterator();
        List<HashMap<String, Object>> result = new ArrayList<>();
        List<Integer> subscribedChannelIds = users.getAllSubscribedChannelIds(user);
        while(channelsIter.hasNext()){
            String channelName = channelsIter.next();
            Channel channel = channels.get(channelName);
            HashMap<String, Object> channelDtls = new HashMap<>();
            channelDtls.put("id", channel.getId());
            channelDtls.put("channel_name", channel.getChannelName());
            channelDtls.put("twitch_id", channel.getTwitchId());
            channelDtls.put("is_user_subscribed", subscribedChannelIds.contains(channel.getId()));
            result.add(channelDtls);
        }
        return result;
    }
}
