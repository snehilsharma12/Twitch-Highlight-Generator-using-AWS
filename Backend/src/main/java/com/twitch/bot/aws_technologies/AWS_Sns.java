package com.twitch.bot.aws_technologies;

import java.util.logging.Logger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishResult;
import com.twitch.bot.sns_model.SnsData;

public class AWS_Sns {
    private static final Logger LOG = Logger.getLogger(AWS_Comprehend.class.getName());
    AWS_Credentials credentials;

    public AWS_Credentials getCredentials(){
        return credentials;
    }

    public AWS_Sns(AWS_Credentials credentials){
        this.credentials = credentials;
    }

    private AmazonSNS getSnsClient(){
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(System.getProperty("aws.accessKeyId"), System.getProperty("aws.secretAccessKey"));
        AWSStaticCredentialsProvider awsStaticCredentialsProvider = new AWSStaticCredentialsProvider(awsCreds);
        return AmazonSNSClientBuilder.standard()
        .withCredentials(awsStaticCredentialsProvider)
        .withRegion(AWS_Credentials.getRegionInRegionsForm())
        .build();
    }

    public void publishSNSMessage(SnsData data) {

        LOG.info("Publishing SNS message: " + data.toString());
    
        PublishResult result = getSnsClient().publish(getSnsTopicArn(), data.toString());
    
        LOG.info("SNS Message ID: " + result.getMessageId());
    }

    private String getSnsTopicArn(){
        return System.getenv("SNS_QUEUE");
    }
}
