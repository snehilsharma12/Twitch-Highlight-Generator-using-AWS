package com.twitch.bot.model;

import com.twitch.bot.db_utils.TwitchData;

public class Channel {
    private Integer id;
    private String channel_name;
    private String twitch_id;
    private Boolean isListeningToChannel;

    public Channel(Integer id, String channel_name, String twitch_id){
        this.id = id;
        this.channel_name = channel_name;
        this.twitch_id = twitch_id;
        this.isListeningToChannel = false;
    }

    public Channel(Integer id, String channel_name, String twitch_id, Boolean isListeningToChannel){
        this.id = id;
        this.channel_name = channel_name;
        this.twitch_id = twitch_id;
        this.isListeningToChannel = isListeningToChannel;
    }
    
    public String getChannelName() {
        return channel_name;
    }

    public Integer getId() {
        return id;
    }

    public String getTwitchId() {
        return twitch_id;
    }

    public Boolean getIsListeningToChannel(){
        return isListeningToChannel;
    }
    

    public void setIsListeningToChannel(Boolean isListeningToChannel, TwitchData twitchData){
        if(twitchData != null){
            this.isListeningToChannel = isListeningToChannel;
            twitchData.updateChannelServerListeningData(isListeningToChannel, id);
        }
    }

    @Override
    public final String toString() {
		return "ID : " + id + ", Channel Name : " + channel_name + ", Twitch Id : " + twitch_id + ", Is the Server Listening to Channel? : " + isListeningToChannel;
	}
}
