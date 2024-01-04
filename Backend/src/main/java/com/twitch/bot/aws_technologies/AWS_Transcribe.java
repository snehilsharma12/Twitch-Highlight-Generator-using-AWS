package com.twitch.bot.aws_technologies;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.AmazonTranscribeClientBuilder;
import com.amazonaws.services.transcribe.model.DeleteTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.Media;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.TranscriptionJob;
import com.amazonaws.services.transcribe.model.TranscriptionJobStatus;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AWS_Transcribe {
    private static final Logger LOG = Logger.getLogger(AWS_Transcribe.class.getName());
    String jobName;
    String s3Key;
    String initiatedJobName;
    AWS_S3 s3Client;
    AWS_Credentials credentials;
    Regions regions;

    public Regions getRegions() {
        return regions;
    }

    public AWS_S3 getS3Client() {
        return s3Client;
    }

    public String getJobName() {
        return jobName;
    }

    public String getInitiatedJobName() {
        return initiatedJobName;
    }

    public AWS_Credentials getCredentials(){
        return credentials;
    }

    public String getS3Key(){
        return s3Key;
    }

    public AWS_Transcribe(String jobName, String s3Key, AWS_S3 s3Client, AWS_Credentials credentials, Regions regions){
        this.jobName = jobName;
        this.s3Client = s3Client;
        this.credentials = credentials;
        this.s3Key = s3Key;
        this.regions = regions;
    }

    private AmazonTranscribe transcribeClient() {
        LOG.log(Level.INFO, "Intialize Transcribe Client");
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(System.getProperty("aws.accessKeyId"), System.getProperty("aws.secretAccessKey"));
        AWSStaticCredentialsProvider awsStaticCredentialsProvider = new AWSStaticCredentialsProvider(awsCreds);
        return AmazonTranscribeClientBuilder.standard().withCredentials(awsStaticCredentialsProvider)
                .withRegion(getRegions()).build();
    }

    public StartTranscriptionJobResult initateTranscribe(){
        Media media = new Media();
        media.withMediaFileUri(s3Client.getFileUrl(getS3Key()));

        StartTranscriptionJobRequest transcriptionReq = new StartTranscriptionJobRequest();
        transcriptionReq.setMedia(media);
        transcriptionReq.setIdentifyLanguage(true);
        transcriptionReq.setTranscriptionJobName(getJobName());

        StartTranscriptionJobResult transcriptionJobResult = transcribeClient().startTranscriptionJob(transcriptionReq);

        this.initiatedJobName = transcriptionJobResult.getTranscriptionJob().getTranscriptionJobName();

        return transcriptionJobResult;
    }

    public void deleteTranscriptionJob() {
        LOG.log(Level.INFO, "Delete Transcription Job from amazon Transcribe {0}",new Object[]{initiatedJobName});
        DeleteTranscriptionJobRequest transcriptionJobDeleteRequest = new DeleteTranscriptionJobRequest()
            .withTranscriptionJobName(initiatedJobName);
        transcribeClient().deleteTranscriptionJob(transcriptionJobDeleteRequest);
    }

    public GetTranscriptionJobResult getTranscriptionJobResult() throws Exception{
        LOG.log(Level.INFO, "Get Transcription Job Result By Job Name : {0} ", new Object[]{initiatedJobName});
        GetTranscriptionJobRequest getTranscriptionJobRequest = new GetTranscriptionJobRequest()
                .withTranscriptionJobName(initiatedJobName);
        Boolean resultFound = false;
        TranscriptionJob transcriptionJob = new TranscriptionJob();
        GetTranscriptionJobResult getTranscriptionJobResult = new GetTranscriptionJobResult();
        while (resultFound == false) {
            getTranscriptionJobResult = transcribeClient().getTranscriptionJob(getTranscriptionJobRequest);
            transcriptionJob = getTranscriptionJobResult.getTranscriptionJob();
            if (transcriptionJob.getTranscriptionJobStatus()
                    .equalsIgnoreCase(TranscriptionJobStatus.COMPLETED.name())) {
                return getTranscriptionJobResult;
            } else if (transcriptionJob.getTranscriptionJobStatus()
                    .equalsIgnoreCase(TranscriptionJobStatus.FAILED.name())) {
                        if(transcriptionJob.getFailureReason() != null && transcriptionJob.getFailureReason().startsWith("Your audio file must have a speech segment")){
                            throw new Exception("NO_AUDIO");
                        }
                return null;
            } else if (transcriptionJob.getTranscriptionJobStatus()
                    .equalsIgnoreCase(TranscriptionJobStatus.IN_PROGRESS.name())) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, "Exception ::: " + ex.getMessage());
                }
            }
        }
        return getTranscriptionJobResult;
    }

    public String downloadTranscriptionResponse(String uri){
        LOG.log(Level.INFO, "Download Transcription Result from Transcribe URi {0}", new Object[]{uri});
        OkHttpClient okHttpClient = new OkHttpClient()
            .newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
        Request request = new Request.Builder().url(uri).build();
        Response response;
        try {
            response = okHttpClient.newCall(request).execute();
            String body = response.body().string();
           // ObjectMapper objectMapper = new ObjectMapper();
            response.close();
            return body;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
