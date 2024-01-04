package com.twitch.bot.model;

public class Subscriptions {
    private Integer userId;
    private Integer channelId;

    public Subscriptions(Integer userId, Integer channelId){
        this.userId = userId;
        this.channelId = channelId;
    }
    
    public Integer getUserId() {
        return userId;
    }

    public Integer getChannelId() {
        return channelId;
    }
}
