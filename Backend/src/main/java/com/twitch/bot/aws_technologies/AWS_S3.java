package com.twitch.bot.aws_technologies;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

public class AWS_S3 {
    private static final Logger LOG = Logger.getLogger(AWS_S3.class.getName());

    String bucketName;

    AWS_Credentials credentials;

    
    public AWS_Credentials getCredentials(){
        return credentials;
    }
    
    public String getBucketName() {
        return bucketName;
    }

    public AWS_S3(String bucketName, AWS_Credentials credentials){
        this.bucketName = bucketName;
        this.credentials = credentials;
    }

    public Boolean isAwsTranscribeBucketExists() {
        S3Client s3Client = S3Client.builder().region(AWS_Credentials.getRegionInRegionForm())
                .credentialsProvider(SystemPropertyCredentialsProvider.create()).build();

        HeadBucketRequest request = HeadBucketRequest.builder()
                .bucket(getBucketName())
                .build();

        try {
            s3Client.headBucket(request);
            return true;
        } catch (Exception ex) {
            return false;
        }

    }

    public void createAwsTranscribeBucket() {
        S3Client s3Client = S3Client.builder().region(AWS_Credentials.getRegionInRegionForm())
        .credentialsProvider(SystemPropertyCredentialsProvider.create()).build();

        CreateBucketConfiguration createBucketConfig;

        if(!AWS_Credentials.getRegionInRegionForm().equals(Region.US_EAST_1)){
            createBucketConfig = CreateBucketConfiguration.builder()
            .locationConstraint(AWS_Credentials.getRegionInRegionForm().id())
            .build();
        }else{
            createBucketConfig = CreateBucketConfiguration.builder()
                .build();
        }

        CreateBucketRequest createBucketRequest = CreateBucketRequest
            .builder()
            .bucket(getBucketName())
            .createBucketConfiguration(createBucketConfig)
            .build();
        
            s3Client.createBucket(createBucketRequest);
    }

    public void deleteAwsTranscribeBucket() {
        S3Client s3Client = S3Client.builder().region(AWS_Credentials.getRegionInRegionForm())
        .credentialsProvider(SystemPropertyCredentialsProvider.create()).build();

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(getBucketName()).build();

        s3Client.deleteBucket(deleteBucketRequest);
    }

    private AmazonS3 getS3(){
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(System.getProperty("aws.accessKeyId"), System.getProperty("aws.secretAccessKey"));
        AWSStaticCredentialsProvider awsStaticCredentialsProvider = new AWSStaticCredentialsProvider(awsCreds);
        return AmazonS3ClientBuilder.standard().withCredentials(awsStaticCredentialsProvider).withRegion(Regions.fromName(AWS_Credentials.getRegionInRegionForm().toString()))
        .build();
    }

    
    public void deleteFileFromAwsBucket(String key) {
        LOG.log(Level.INFO, "Delete File from AWS Bucket {0}", new Object[]{key});
        getS3().deleteObject(getBucketName(), key);
    }

    public void uploadFileToAwsBucket(String key, InputStream stream) {
        LOG.log(Level.INFO, "Upload File to AWS Bucket {0}", new Object[]{key});
        getS3().putObject(getBucketName(), key, stream, null);
    }

    public String getFileUrl(String key){
        return getS3().getUrl(getBucketName(), key).toExternalForm();
    }
}
