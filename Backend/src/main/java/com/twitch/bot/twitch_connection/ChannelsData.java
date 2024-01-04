package com.twitch.bot.twitch_connection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

import com.twitch.bot.db_utils.TwitchData;
import com.twitch.bot.model.Channel;

@Component
public class ChannelsData {
    private static final Logger LOG = Logger.getLogger(ChannelsData.class.getName());
    private static HashMap<String, Channel> channels = new HashMap<>();
    TwitchData twitchData;

    public static HashMap<String, Channel> getChannels() {
        return channels;
    }

    static void setChannelDetails(List<Channel> channelsData){
        Iterator<Channel> channelsDataIter = channelsData.iterator();
        while(channelsDataIter.hasNext()){
            Channel channel = channelsDataIter.next();
            channels.put(channel.getChannelName(), channel);
        }
    }

    public ChannelsData(TwitchData twitchData){
        this.twitchData = twitchData;
        if(channels == null || channels.isEmpty()){
            setChannelDetails(twitchData.getChannelDetails());
        } 
    }

    public static Channel getChannel(Integer channelId) {
        HashMap<String, Channel> channels = getChannels();
        Iterator<String> channelsIter = channels.keySet().iterator();
        while(channelsIter.hasNext()){
            String channelName = channelsIter.next();
            Channel channelInfo = channels.get(channelName);
            if(channelInfo.getId().equals(channelId)){
                return channelInfo;
            }
        }
        return null;
    }

    public static Channel getChannel(String channel) {
        return channels.get(channel);
    }

    public Channel addChannel(String channelName, String channelId){
        Channel channel = null;
        channel = this.twitchData.getChannelDetails(channelName, channelId);
        if(channel == null){
            channel = this.twitchData.addChannelDetails(channelName, channelId);
        }
        if(channel != null){
            channels.put(channel.getChannelName(), channel);
        }
        return channel;
    }

    public void removeChannel(Channel channel){
        this.twitchData.deleteChannelDetails(channel.getId());
    }

    public static Channel joinChannel(String channelName, Connection twitch_bot) {
        Channel channel = channels.get(channelName);
        twitch_bot.sendCommandMessage("JOIN " + "#" + channelName + "\r\n");
        LOG.log(Level.INFO, "> JOIN " + channelName);
        channel.setIsListeningToChannel(true, twitch_bot.getTwitchData());
        channels.put(channelName, channel);
        return channel;
    }

    public static void stopListeningToChannel(String channelName, Connection twitch_bot) {
        Channel channel = channels.get(channelName);
        twitch_bot.sendCommandMessage("PART " + "#" + channelName);
        channel.setIsListeningToChannel(false, twitch_bot.getTwitchData());
        channels.put(channelName, channel);
        LOG.log(Level.INFO, "> PART " + channelName);
    }
}
