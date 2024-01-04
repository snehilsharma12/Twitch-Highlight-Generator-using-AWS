package com.twitch.bot.twitch_connection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.twitch.bot.db_utils.TwitchAWS_RDS;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.Subscriptions;
import com.twitch.bot.model.User;

@Component
public class Users {
    TwitchAWS_RDS twitchAWS_RDS;
    public Users(TwitchAWS_RDS twitchAWS_RDS){
        this.twitchAWS_RDS = twitchAWS_RDS;
    }

    public Boolean authenticateUser(String username, String password, Boolean isUserName) throws Exception{
        User user = twitchAWS_RDS.getUserDetails(username, password, isUserName);
        return (user != null);
    }

    public Boolean checkIfEmailOrUserNamePresent(String name, Boolean isUserName) throws Exception{
        User user = twitchAWS_RDS.getUserDetails(name, isUserName);
        return (user != null);
    }

    public Boolean authenticateUser(Integer userId) throws Exception{
        User user = twitchAWS_RDS.getUserDetails(userId);
        return (user != null);
    }

    public User getUserDetails(String username, String password, Boolean isUserName) throws Exception{
        return twitchAWS_RDS.getUserDetails(username, password, isUserName);
    }

    public User getUserDetails(Integer userId) throws Exception{
        return twitchAWS_RDS.getUserDetails(userId);
    }

    public User registerUser(String username, String password, String email) throws Exception{
        if(!checkIfEmailOrUserNamePresent(email, false)){
            return twitchAWS_RDS.addUserDetails(username, email, password);
        }else{
            throw new Exception("User Already Present");
        }
    }

    public List<Channel> getUserSubscribedChannels(User user) throws Exception{
        List<Subscriptions> subscriptions = twitchAWS_RDS.getSubscriptionDetailsBasedOnUserOrSubscriptionId(user.getUserId(), true); 
        Iterator<Subscriptions> subscriptionsIter = subscriptions.iterator();
        List<Channel> subscribedChannels = new ArrayList<>();
        while(subscriptionsIter.hasNext()){
            Subscriptions subscription = subscriptionsIter.next();
            Channel channel = ChannelsData.getChannel(subscription.getChannelId());
            subscribedChannels.add(channel);
        }
        return subscribedChannels;
    }

    public Subscriptions checkAndAddUserSubscriptions(Integer userId, Integer channelId) throws Exception{
        if(authenticateUser(userId)){
            Channel channel = ChannelsData.getChannel(channelId);
            if(channel != null){
                return addUserSubscriptions(getUserDetails(userId), channel);
            }
        }
        return null;
    }

    public Boolean checkAndDeleteUserSubscriptions(Integer userId, Integer channelId) throws Exception{
        if(authenticateUser(userId)){
            Channel channel = ChannelsData.getChannel(channelId);
            User user = getUserDetails(userId);
            if(channel != null && user != null){
                return deleteUserSubscriptions(user, channel);
            }
        }
        return false;
    }

    public Boolean isUserSubscribedToChannel(User user, Channel channel) throws Exception{
        return twitchAWS_RDS.checkIfSubscriptionExists(user.getUserId(), channel.getId());
    }

    public Subscriptions addUserSubscriptions(User user, Channel channel) throws Exception{
        if(isUserSubscribedToChannel(user, channel)){
            throw new Exception("Subscription Already Present");
        }else{
            return twitchAWS_RDS.addSubscriptionDetails(user, channel);
        }
    }

    public Boolean deleteUserSubscriptions(User user, Channel channel) throws Exception{
        if(!isUserSubscribedToChannel(user, channel)){
            throw new Exception("Subscription Not Present");
        }else{
            Subscriptions subscriptions = new Subscriptions(user.getUserId(), channel.getId());
            return twitchAWS_RDS.deleteSubscriptionDetails(subscriptions);
        }
    }

    public List<Integer> getAllSubscribedChannelIds(User user) throws Exception{
        List<Subscriptions> subscriptions = twitchAWS_RDS.getSubscriptionDetailsBasedOnUserOrSubscriptionId(user.getUserId(), true);
        List<Integer> channelIds = new ArrayList<>();
        Iterator<Subscriptions> subscriptionsIter = subscriptions.iterator();
        while(subscriptionsIter.hasNext()){
            Subscriptions subscription = subscriptionsIter.next();
            channelIds.add(subscription.getChannelId());
        }
        return channelIds;
    }
}
