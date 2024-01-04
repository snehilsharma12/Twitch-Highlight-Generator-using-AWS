package com.twitch.bot.sns_model;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SnsData {
    private List<Integer> userId;
    private String channelName;
    private Integer channelId;
    public List<Integer> getUserId() {
        return userId;
    }
    public void setUserId(List<Integer> userId) {
        this.userId = userId;
    }
    public String getChannelName() {
        return channelName;
    }
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
    public Integer getChannelId() {
        return channelId;
    }
    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    @Override
    public String toString(){
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
           return null;
        }
    }
    
}
