package com.twitch.bot.aws_technologies;

import com.amazonaws.regions.Regions;

import software.amazon.awssdk.regions.Region;

public class AWS_Credentials {
    String access_key;
    String access_id;

    public String getAccess_key() {
        return access_key;
    }
    public String getAccess_id() {
        return access_id;
    }

    public AWS_Credentials(String access_key, String access_id){
        this.access_key = access_key;
        this.access_id = access_id;

        System.setProperty("aws.accessKeyId", access_id);
        System.setProperty("aws.secretAccessKey", access_key);
    }

    public static Region getRegionInRegionForm(){
        String regionName = System.getenv("AWS_REGION");
        return Region.of(regionName);
    }

    public static Regions getRegionInRegionsForm(){
        String regionName = System.getenv("AWS_REGION");
        return Regions.fromName(regionName);
    }
}
