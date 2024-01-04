package com.twitch.bot.db_utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.twitch.bot.dynamo_db_model.Messages;
import com.twitch.bot.dynamo_db_model.MessagesCount;
import com.twitch.bot.dynamo_db_model.TwitchAnalysis;
import com.twitch.bot.dynamo_db_model.TwitchAnalysis.ClipsDetails;
import com.twitch.bot.model.Channel;

import static com.mongodb.client.model.Filters.eq;

@Component
@DependsOn({"twitchAWS_RDS", "twitchAWS_DynamoDB"})
public class TwitchData {
    private static final Logger LOG = Logger.getLogger(TwitchData.class.getName());
    MongoClient mongoClient;
    AmazonDynamoDB dynamoDb;
    TwitchAWS_DynamoDB twitchDynamoDB;
    TwitchAWS_RDS twitchRdsDB;
    private static String dbPassword;
    Integer messageMinsFrequency;
    String hourMinSeperator;

    public Integer getMessageMinsFrequency() {
        return messageMinsFrequency;
    }

    public enum DYNAMODB_TABLES {
        MESSAGES("Messages"),
        MESSAGE_COUNT_ROLLING_WINDOW("Message_Count_Rolling_Window"),
        TWITCH_ANALYSIS("Twitch_Analysis");

        String tableName;

        DYNAMODB_TABLES(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public String toString() {
            return this.tableName;
        }
    }

    public TwitchData(@Value("true") Boolean isCalledOnInitalize, @Value("${rolling.message.minutes.frequency}") Integer messageMinsFrequency,
            @Value("${hour.minutes.seperator}") String hourMinSeperator, TwitchAWS_RDS twitchRdsDB, TwitchAWS_DynamoDB twitchDynamoDB) {
        if (isAwsEnvironment()) {
            LOG.log(Level.INFO, "inside AWS Environment");
            this.twitchDynamoDB = twitchDynamoDB;
            this.twitchRdsDB = twitchRdsDB;
        }
        if (isCalledOnInitalize) {
            setDbPassword(System.getenv("MONGODB_PASSWORD"));
        }
        makeConnectionToDB();
        this.messageMinsFrequency = messageMinsFrequency;
        this.hourMinSeperator = hourMinSeperator;
    }

    public static void setDbPassword(String dbPassword) {
        TwitchData.dbPassword = dbPassword;
    }

    public static Boolean isAwsEnvironment() {
        return System.getenv("AWS_ENVIRONMENT") != null ? Boolean.valueOf(System.getenv("AWS_ENVIRONMENT").toString())
                : false;
    }

    public void makeConnectionToDB() {
        String connectionData = "mongodb+srv://twitch:"
                + dbPassword
                + "@twitchprojectoauth.lfctwou.mongodb.net/?retryWrites=true&w=majority";
        ConnectionString connectionString = new ConnectionString(connectionData);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        mongoClient = MongoClients.create(settings);
    }

    public JSONObject getTwitchCredentials() {
        return getTwitchCredentialsFromMongoDB();
    }

    private JSONObject getTwitchCredentialsFromMongoDB() {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("credentials");
        JSONObject data = new JSONObject();
        FindIterable<Document> iterDoc = collection.find();
        Iterator<Document> it = iterDoc.iterator();
        while (it.hasNext()) {
            Document document = it.next();
            JSONObject documentData = new JSONObject(document.toJson());
            data.put("access_token", documentData.getString("access_token"));
            data.put("refresh_token", documentData.getString("refresh_token"));
            data.put("client_id", documentData.getString("client_id"));
            data.put("client_secret", documentData.getString("client_secret"));
            data.put("user_name", documentData.getString("user_name"));
        }
        return data;
    }

    public Boolean setTwitchCredentials(JSONObject data) {
        return setTwitchCredentialsToMongoDB(data);
    }

    private Boolean setTwitchCredentialsToMongoDB(JSONObject data) {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("credentials");
        if (!data.has("client_id")) {
            LOG.info("Client Id not present in given data ::: " + data.toString());
            return false;
        }
        if (data.has("access_token")) {
            collection.updateOne(eq("client_id", data.get("client_id").toString()),
                    Updates.set("access_token", data.get("access_token").toString()));
        }
        if (data.has("refresh_token")) {
            collection.updateOne(eq("client_id", data.get("client_id").toString()),
                    Updates.set("refresh_token", data.get("refresh_token").toString()));
        }
        return true;
    }

    public void addTwitchMessage(String user, Channel channel, String message, Long timeStamp) {
        if (null == timeStamp) {
            timeStamp = System.currentTimeMillis();
        }
        twitchDynamoDB.addTwitchMessage(user, channel, message, timeStamp);
    }

    public JSONArray getTwitchMessageForChannel(Channel channel) {
        return getTwitchMessageForChannel(channel, null, null, null);
    }

    public JSONArray getTwitchMessageForChannel(Channel channel, String user) {
        return getTwitchMessageForChannel(channel, user, null, null);
    }

    public JSONArray getTwitchMessageForChannel(Channel channel, Long fromTimeStamp, Long toTimeStamp) {
        return getTwitchMessageForChannel(channel, null, fromTimeStamp, toTimeStamp);
    }

    public JSONArray getTwitchMessageForChannel(Channel channel, String user, Long fromTimeStamp, Long toTimeStamp) {
        Messages message = new Messages();
        message.setId(null);
        message.setChannelName(channel.getChannelName());
        message.setMessage(null);
        message.setUserName(user);
        message.setTimestamp(null);
        return twitchDynamoDB.getTwitchMessageForChannelInJSONFormat(message, fromTimeStamp, toTimeStamp);
    }

    public void deleteTwitchMessageForChannel(Channel channel) {
        deleteTwitchMessageForChannel(channel, null, null);
    }

    public void deleteTwitchMessageForChannel(Channel channel, Long timeStamp) {
        deleteTwitchMessageForChannel(channel, null, timeStamp);
    }

    public void deleteTwitchMessageForChannel(Channel channel, String user) {
        deleteTwitchMessageForChannel(channel, user, null);
    }

    public void deleteTwitchMessageForChannel(Channel channel, String user, Long toTimeStamp) {
        Messages message = new Messages();
        message.setId(null);
        message.setChannelName(channel.getChannelName());
        message.setMessage(null);
        message.setUserName(user);
        message.setTimestamp(null);
        twitchDynamoDB.deleteTwitchMessageForChannel(message, null, toTimeStamp);
    }

    public List<Channel> getChannelDetails() {
        try{
            return twitchRdsDB.getAllChannels();
        }catch(Exception ex){
            LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
        }
        return null;
    }


    public Channel addChannelDetails(String channelName, String channelId) {
        try {
            return twitchRdsDB.addChannelDetails(channelName, channelId, false);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
        }
        return null;
    }

    public Channel getChannelDetails(String channelName, String channelId) {
        try {
            return twitchRdsDB.getChannelDetails(channelName, channelId);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
        }
        return null;
    }

    public void deleteChannelDetails(Integer id) {
        try {
            twitchRdsDB.deleteChannelDetails(twitchRdsDB.getChannelDetails(id));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
        }
    }

    public void deleteChannelRelatedInfo(Channel channel) throws Exception{
        deleteTwitchMessageForChannel(channel);
        twitchDynamoDB.deleteChannelRelatedInfo(channel);
        twitchRdsDB.deleteSubscriptionDetailsForAChannel(channel);
        twitchRdsDB.deleteChannelDetails(channel);
    }

    public void updateChannelServerListeningData(Boolean isServerListening, Integer channelId) {
        try {
            twitchRdsDB.updateChannelListenDetails(channelId, isServerListening);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
        }
    }

    public JSONArray getTwitchAnalysisOfAChannel(Channel channel, Boolean isAscendingOrder) {
        return twitchDynamoDB.getTwitchAnalysisOfAChannelInJSON(channel, isAscendingOrder);
    }

    public void addTwitchAnalysis(Channel channel, String sentimental_result, String video_sentimental_result, ClipsDetails clip_details, Long timeStamp) {
        if(timeStamp == null){
            timeStamp = System.currentTimeMillis();
        }
        twitchDynamoDB.addTwitchAnalysisInDynamoDB(channel, sentimental_result, video_sentimental_result, clip_details, timeStamp);
    }

    public JSONObject getCloudCredentials() {
        return getCloudCredentialsFromAWS();
    }

    private JSONObject getCloudCredentialsFromAWS() {
        return twitchDynamoDB.getCloudCredentialsFromAWS();
    }

    public void addMessageCountBasedOnRollingWindow(Channel channel, Long messageCount, Long timestamp) {
        Integer[] hoursAndMinutes = getHoursAndMinutes(timestamp);
        MessagesCount messageCountObj = new MessagesCount();
        messageCountObj.setHourMinutesKey(getHoursAndMinutesKey(hoursAndMinutes));
        messageCountObj.setChannelName(channel.getChannelName());
        List<MessagesCount> messagesData = twitchDynamoDB.getMessageCount(messageCountObj);
        if (messagesData.isEmpty()) {
            messageCountObj = new MessagesCount();
            messageCountObj.setChannelName(channel.getChannelName());
            messagesData = manipulateMessagesDataBasedOnMessageFreq(twitchDynamoDB.getMessageCount(messageCountObj), timestamp);
            twitchDynamoDB.addMessageCountToDynamoDB(channel, messageCount, getHoursAndMinutesKey(hoursAndMinutes));
        } else {
            messageCountObj = messagesData.get(0);
            twitchDynamoDB.updateMessageCountToDynamoDB(messageCountObj, messageCount);
        }

    }

    private List<MessagesCount> manipulateMessagesDataBasedOnMessageFreq(List<MessagesCount> messagesData, Long currentTimestamp){
        Integer[] currentHoursAndMinutes = getHoursAndMinutes(currentTimestamp);
        List<MessagesCount> toBeDeletedMessagesData = new ArrayList<>();
        Iterator<MessagesCount> messagesDataIter = messagesData.iterator();
        while(messagesDataIter.hasNext()){
            MessagesCount msgCountData = messagesDataIter.next();
            String hourMinKey = msgCountData.getHourMinutesKey();
            String[] hourMinKeyArr = hourMinKey.split(hourMinSeperator);
            Integer hourMin[] = new Integer[hourMinKeyArr.length];
            for (int i = 0; i < hourMinKeyArr.length; i++) {
                hourMin[i] = Integer.parseInt(hourMinKeyArr[i]);
             }
             if(hourMin[0] == currentHoursAndMinutes[0]){
                if(currentHoursAndMinutes[1] - messageMinsFrequency > hourMin[1]){
                    toBeDeletedMessagesData.add(msgCountData);
                }
             }else if(hourMin[0] == (currentHoursAndMinutes[0] - 1) || (currentHoursAndMinutes[0] == 0 && hourMin[0] == 23)){
                Integer remainingMins = currentHoursAndMinutes[1] - messageMinsFrequency;
                if(remainingMins >= 0){
                    toBeDeletedMessagesData.add(msgCountData);
                }else{
                    if((60 - remainingMins) >  hourMin[1]){
                        toBeDeletedMessagesData.add(msgCountData);
                    }
                }
             }else{
                toBeDeletedMessagesData.add(msgCountData);
             }
        }

        if (!toBeDeletedMessagesData.isEmpty()) {
            twitchDynamoDB.deleteMessageCount(toBeDeletedMessagesData);
            MessagesCount messageCountObj = new MessagesCount();
            messageCountObj.setHourMinutesKey(getHoursAndMinutesKey(currentHoursAndMinutes));
            messageCountObj.setChannelName(toBeDeletedMessagesData.get(0).getChannelName());
            return twitchDynamoDB.getMessageCount(messageCountObj);
        }
        return messagesData;
    }

    public List<TwitchAnalysis> getTwitchAnalysisRawDataOfAChannel(Channel channel, Boolean isAscending) {
        return twitchDynamoDB.getTwitchAnalysisOfAChannel(channel, isAscending);
    }

    public List<MessagesCount> getMessageCountDataOfAChannel(Channel channel){
        return twitchDynamoDB.getMessageCountDataOfAChannel(channel);
    }

    public void clearMessagesCountForAChannel(Channel channel){
        twitchDynamoDB.clearMessagesCountForAChannel(channel);
    }

    public String getHoursAndMinutesKey(Integer[] hours_minutes){
        return hours_minutes[0] + hourMinSeperator + hours_minutes[1];
    }

    public Integer[] getHoursAndMinutes(Long timeStamp){
        Timestamp stamp = new Timestamp(timeStamp);
        Date date = new Date(stamp.getTime());

        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        return new Integer[]{hour, minute};
    }
}
