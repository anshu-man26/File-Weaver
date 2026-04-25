package com.fileweaver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${app.aws.region:ap-south-1}")
    private String region;

    /** Set for LocalStack — leave blank in prod. */
    @Value("${app.aws.endpoint:}")
    private String endpoint;

    @Value("${app.aws.access-key:}")
    private String accessKey;

    @Value("${app.aws.secret-key:}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        var b = S3Client.builder().region(Region.of(region));
        if (!endpoint.isBlank()) {
            b.endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        b.credentialsProvider(credentials());
        return b.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var b = S3Presigner.builder().region(Region.of(region));
        if (!endpoint.isBlank()) {
            b.endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        b.credentialsProvider(credentials());
        return b.build();
    }

    @Bean
    public SqsClient sqsClient() {
        var b = SqsClient.builder().region(Region.of(region));
        if (!endpoint.isBlank()) {
            b.endpointOverride(URI.create(endpoint));
        }
        b.credentialsProvider(credentials());
        return b.build();
    }

    private software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credentials() {
        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.create();
    }
}
