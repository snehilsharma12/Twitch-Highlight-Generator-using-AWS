package com.twitch.bot.aws_technologies;

import java.util.logging.Level;
import java.util.logging.Logger;

import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.ComprehendException;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentRequest;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentResponse;

public class AWS_Comprehend {
    private static final Logger LOG = Logger.getLogger(AWS_Comprehend.class.getName());
    String message;
    AWS_Credentials credentials;

    public AWS_Credentials getCredentials(){
        return credentials;
    }

    public String getMessage() {
        return message;
    }

    public AWS_Comprehend(String message, AWS_Credentials credentials){
        this.message = message;
        this.credentials = credentials;
    }

    public String getSentiment() throws Exception{
       
        ComprehendClient comClient = ComprehendClient.builder()
            .region(AWS_Credentials.getRegionInRegionForm()).credentialsProvider(SystemPropertyCredentialsProvider.create())
            .build();

        try {
            DetectSentimentRequest detectSentimentRequest = DetectSentimentRequest.builder()
                .text(getMessage())
                .languageCode("en")
                .build();

            DetectSentimentResponse detectSentimentResult = comClient.detectSentiment(detectSentimentRequest);
            return detectSentimentResult.sentimentAsString();

        } catch (ComprehendException ex) {
            LOG.log(Level.WARNING, "Exception is ::: ", ex);
            throw ex;
        }
    }
}
