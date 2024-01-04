package com.twitch.bot.db_utils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.twitch.bot.db_utils.TwitchAWS_RDSConnection.TWITCH_STREAMERS;
import com.twitch.bot.db_utils.TwitchAWS_RDSConnection.USERS;
import com.twitch.bot.db_utils.TwitchAWS_RDSConnection.USER_SUBSCRIPTION;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.Subscriptions;
import com.twitch.bot.model.User;

@Component
public class TwitchAWS_RDS {
    private static final Logger LOG = Logger.getLogger(TwitchAWS_RDS.class.getName());
    TwitchAWS_RDSConnection rdsConnection;
    public TwitchAWS_RDS(TwitchAWS_RDSConnection rdsConnection){
        this.rdsConnection = rdsConnection;
    }

    public List<User> getAllUsers() throws Exception{
        ResultSet result = rdsConnection.getAllUsersRecord();
        List<User> users = new ArrayList<>();
        while(result.next()){
            users.add(getUserObjectFromResultSet(result));
        }
        return users;
    }

    public User getUserDetails(String emailOrName, String password, Boolean isName) throws Exception{
        String filterCondition = "";
        if(isName){
            filterCondition = USERS.COLUMN_NAME.toString() + " = " + rdsConnection.addStringLiteralToString(emailOrName);
        }else{
            filterCondition = USERS.COLUMN_EMAIL.toString() + " = " + rdsConnection.addStringLiteralToString(emailOrName);
        }
        filterCondition += " " + TwitchAWS_RDSConnection.AND + " " + USERS.COLUMN_PASSWORD.toString() + " = " + rdsConnection.addStringLiteralToString(password);

        ResultSet result = rdsConnection.getUsersRecordBasedOnCriteria(rdsConnection.getAllUsersColumns(), filterCondition);
        while(result.next()){
            return getUserObjectFromResultSet(result);
        }
        return null;
    }

    public User getUserDetails(String emailOrName, Boolean isName) throws Exception{
        String filterCondition = "";
        if(isName){
            filterCondition = USERS.COLUMN_NAME.toString() + " = " + rdsConnection.addStringLiteralToString(emailOrName);
        }else{
            filterCondition = USERS.COLUMN_EMAIL.toString() + " = " + rdsConnection.addStringLiteralToString(emailOrName);
        }

        ResultSet result = rdsConnection.getUsersRecordBasedOnCriteria(rdsConnection.getAllUsersColumns(), filterCondition);
        while(result.next()){
            return getUserObjectFromResultSet(result);
        }
        return null;
    }

    public User getUserDetails(Integer userId) throws Exception{
        String filterCondition = "";
        filterCondition = USERS.COLUMN_ID.toString() + " = " + userId;

        ResultSet result = rdsConnection.getUsersRecordBasedOnCriteria(rdsConnection.getAllUsersColumns(), filterCondition);
        while(result.next()){
            return getUserObjectFromResultSet(result);
        }
        return null;
    }

    public User addUserDetails(String name, String email, String password) throws Exception{
        if(ifGivenObjectIsValid(name) && ifGivenObjectIsValid(email) && ifGivenObjectIsValid(password)){
            Integer id = rdsConnection.createUserRecord(name, email, password);
            return getUserDetails(id);
        }
        return null;
    }

    public Boolean updateUserDetails(User user) throws Exception{
        if(!ifGivenObjectIsValid(user.getUserId())){
            return false;
        }
        String filterCondition = USERS.COLUMN_ID.toString() + " = " + user.getUserId();
        JSONObject data = new JSONObject();
        if(ifGivenObjectIsValid(user.getName())){
            data.put(USERS.COLUMN_NAME.toString(), user.getName());
        }
        if(ifGivenObjectIsValid(user.getEmail())){
            data.put(USERS.COLUMN_EMAIL.toString(), user.getName());
        }
        if(ifGivenObjectIsValid(user.getPassword())){
            data.put(USERS.COLUMN_PASSWORD.toString(), user.getName());
        }
        if(!data.isEmpty()){
            return rdsConnection.updateUsersRecord(data, filterCondition);
        }
        return false;
    }

    public Boolean deleteUserDetails(User user) throws Exception {
        if (!ifGivenObjectIsValid(user.getUserId())) {
            return false;
        }
        String filterCondition = USERS.COLUMN_ID.toString() + " = " + user.getUserId();
        return rdsConnection.deleteUsersRecord(filterCondition);
    }

    public List<Subscriptions> getAllSubscriptions() throws Exception{
        ResultSet result = rdsConnection.getAllUsersSubscriptionRecord();
        List<Subscriptions> subscriptions = new ArrayList<>();
        while(result.next()){
            subscriptions.add(getSubscriptionsFromResultSet(result));
        }
        return subscriptions;
    }

    public List<Subscriptions> getSubscriptionDetailsBasedOnUserOrSubscriptionId(Integer id, Boolean isUserId) throws Exception{
        String filterCondition = "";
        if(isUserId){
            filterCondition = USER_SUBSCRIPTION.COLUMN_USER_ID.toString() + " = " + id;
        }else{
            filterCondition = USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString() + " = " + id;
        }

       List<Subscriptions> data = new ArrayList<>();

        ResultSet result = rdsConnection.getUsersSubscriptionRecordBasedOnCriteria(rdsConnection.getAllUsersSubscriptionColumns(), filterCondition);
        while(result.next()){
            data.add(getSubscriptionsFromResultSet(result));
        }
        return data;
    }

    public Boolean checkIfSubscriptionExists(Integer userId, Integer channelId) throws Exception {
        String filterCondition = "";
        filterCondition = USER_SUBSCRIPTION.COLUMN_USER_ID.toString() + " = " + userId;
        filterCondition += " " + TwitchAWS_RDSConnection.AND + " "
                + USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString() + " = " + channelId;

        List<Subscriptions> data = new ArrayList<>();

        ResultSet result = rdsConnection.getUsersSubscriptionRecordBasedOnCriteria(
                rdsConnection.getAllUsersSubscriptionColumns(), filterCondition);
        while (result.next()) {
            return true;
        }
        return false;
    }

    public Subscriptions getSubscriptionDetails(Integer id) throws Exception{
        String filterCondition = "";
        filterCondition = USER_SUBSCRIPTION.COLUMN_PRIMARY.toString() + " = " + id;

        ResultSet result = rdsConnection.getUsersSubscriptionRecordBasedOnCriteria(rdsConnection.getAllUsersSubscriptionColumns(), filterCondition);
        while(result.next()){
            return getSubscriptionsFromResultSet(result);
        }
        return null;
    }

    public Subscriptions addSubscriptionDetails(User user, Channel channel) throws Exception{
        if(ifGivenObjectIsValid(user.getUserId()) && ifGivenObjectIsValid(channel.getId())){
            Integer id = rdsConnection.createUsersSubscriptionRecord(user.getUserId(), channel.getId());
            return getSubscriptionDetails(id);
        }
        return null;
    }

    public Boolean deleteSubscriptionDetails(Subscriptions subscription) throws Exception {
        if(ifGivenObjectIsValid(subscription.getUserId()) && ifGivenObjectIsValid(subscription.getChannelId())){
            return false;
        }
        String filterCondition = USER_SUBSCRIPTION.COLUMN_USER_ID.toString() + " = " + subscription.getUserId() + " " + TwitchAWS_RDSConnection.AND + " " + USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString() + " = " + subscription.getChannelId();
        return rdsConnection.deleteUsersSubscriptionRecord(filterCondition);
    }

    public Boolean deleteSubscriptionDetailsForAChannel(Channel channel) throws Exception {
        if(ifGivenObjectIsValid(channel.getId())){
            return false;
        }
        String filterCondition = USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString() + " = " + channel.getId();
        return rdsConnection.deleteUsersSubscriptionRecord(filterCondition);
    }

    public List<Channel> getAllChannels() throws Exception{
        ResultSet result = rdsConnection.getAllTwitchStreamersRecord();
        List<Channel> channels = new ArrayList<>();
        while(result.next()){
            channels.add(getChannelFromResultSet(result));
        }
        return channels;
    }

    public Channel getChannelDetails(String channelName, String twitchId) throws Exception{
        String filterCondition = "";
        if(ifGivenObjectIsValid(channelName)){
            filterCondition = TWITCH_STREAMERS.COLUMN_NAME + " = " + rdsConnection.addStringLiteralToString(channelName);
        }
        if(ifGivenObjectIsValid(twitchId)){
            if(filterCondition.trim() != ""){
                filterCondition += " " + TwitchAWS_RDSConnection.AND + " ";
            }
            filterCondition += TWITCH_STREAMERS.COLUMN_TWITCH_ID + " = " + twitchId;
        }


        ResultSet result = rdsConnection.getTwitchStreamersRecordBasedOnCriteria(rdsConnection.getAllTwitchStreamersColumns(), filterCondition);
        while(result.next()){
            return getChannelFromResultSet(result);
        }
        return null;
    }

    public Channel getChannelDetails(Integer id) throws Exception{
        String filterCondition = "";
        filterCondition = TWITCH_STREAMERS.COLUMN_ID.toString() + " = " + id;

        ResultSet result = rdsConnection.getTwitchStreamersRecordBasedOnCriteria(rdsConnection.getAllTwitchStreamersColumns(), filterCondition);
        while(result.next()){
            return getChannelFromResultSet(result);
        }
        return null;
    }

    public Channel addChannelDetails(String channelName, String twitchId, Boolean isListeningToChannel) throws Exception{
        if(ifGivenObjectIsValid(channelName) && ifGivenObjectIsValid(twitchId) && ifGivenObjectIsValid(isListeningToChannel)){
            Integer id = rdsConnection.createTwitchStreamersRecord(channelName, twitchId, isListeningToChannel);
            return getChannelDetails(id);
        }
        return null;
    }

    public Boolean updateChannelDetails(Channel channel) throws Exception{
        if(!ifGivenObjectIsValid(channel.getId())){
            return false;
        }
        String filterCondition = TWITCH_STREAMERS.COLUMN_ID.toString() + " = " + channel.getId();
        JSONObject data = new JSONObject();
        if(ifGivenObjectIsValid(channel.getChannelName())){
            data.put(TWITCH_STREAMERS.COLUMN_NAME.toString(), channel.getChannelName());
        }
        if(ifGivenObjectIsValid(channel.getTwitchId())){
            data.put(TWITCH_STREAMERS.COLUMN_TWITCH_ID.toString(), channel.getTwitchId());
        }
        if(ifGivenObjectIsValid(channel.getIsListeningToChannel())){
            data.put(TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString(), channel.getIsListeningToChannel());
        }
        if(!data.isEmpty()){
            return rdsConnection.updateTwitchStreamersRecord(data, filterCondition);
        }
        return false;
    }

    public Boolean updateChannelListenDetails(Integer channelId, Boolean isListeningToChannel) throws Exception{
        if(!ifGivenObjectIsValid(channelId)){
            return false;
        }
        String filterCondition = TWITCH_STREAMERS.COLUMN_ID.toString() + " = " + channelId;
        JSONObject data = new JSONObject();
        if(ifGivenObjectIsValid(isListeningToChannel)){
            data.put(TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString(), isListeningToChannel);
        }
        if(!data.isEmpty()){
            return rdsConnection.updateTwitchStreamersRecord(data, filterCondition);
        }
        return false;
    }

    public Boolean deleteChannelDetails(Channel channel) throws Exception {
        if (!ifGivenObjectIsValid(channel.getId())) {
            return false;
        }
        String filterCondition = USERS.COLUMN_ID.toString() + " = " + channel.getId();
        return rdsConnection.deleteTwitchStreamersRecord(filterCondition);
    }

    private Boolean ifGivenObjectIsValid(Object data){
        if(data instanceof String){
            return data != null && data.toString().trim() != "";
        }else if(data instanceof Integer){
            return data != null;
        }else if(data instanceof Long){
            return data != null;
        }else if(data instanceof Float){
            return data != null;
        }else if(data instanceof Double){
            return data != null;
        }else if(data instanceof Boolean){
            return data != null;
        }
        return false;
    }

    private User getUserObjectFromResultSet(ResultSet result) throws Exception{
        return new User(result.getInt(USERS.COLUMN_ID.toString()), result.getString(USERS.COLUMN_NAME.toString()), result.getString(USERS.COLUMN_EMAIL.toString()), result.getString(USERS.COLUMN_PASSWORD.toString()));
    }

    private Subscriptions getSubscriptionsFromResultSet(ResultSet result) throws Exception{
        return new Subscriptions(result.getInt(USER_SUBSCRIPTION.COLUMN_USER_ID.toString()), result.getInt(USER_SUBSCRIPTION.COLUMN_TWITCH_STREAMERS_ID.toString()));
    }

    private Channel getChannelFromResultSet(ResultSet result) throws Exception{
        return new Channel(result.getInt(TWITCH_STREAMERS.COLUMN_ID.toString()), result.getString(TWITCH_STREAMERS.COLUMN_NAME.toString()), result.getString(TWITCH_STREAMERS.COLUMN_TWITCH_ID.toString()), Boolean.valueOf(result.getString(TWITCH_STREAMERS.COLUMN_IS_LISTENING_TO_CHANNEL.toString())));
    }
}
