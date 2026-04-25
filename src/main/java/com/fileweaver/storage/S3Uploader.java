package com.fileweaver.storage;

import com.fileweaver.worker.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;

@Service
public class S3Uploader {

    private static final Logger log = LoggerFactory.getLogger(S3Uploader.class);

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${app.s3.bucket}")
    private String bucket;

    public S3Uploader(S3Client s3, S3Presigner presigner) {
        this.s3 = s3;
        this.presigner = presigner;
    }

    public void upload(String key, byte[] bytes, String contentType) {
        try {
            s3.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build(),
                RequestBody.fromBytes(bytes)
            );
            log.debug("Uploaded {} bytes to s3://{}/{}", bytes.length, bucket, key);
        } catch (S3Exception e) {
            if (e.statusCode() >= 500) {
                throw new RetryableException("S3 5xx during upload of " + key, e);
            }
            throw e;
        }
    }

    public PresignedUrl presignDownload(String key, Duration ttl) {
        return presignDownload(key, ttl, null);
    }

    public PresignedUrl presignDownload(String key, Duration ttl, String filenameOverride) {
        // Tell S3 to set Content-Disposition: attachment so the browser
        // downloads the file rather than rendering it inline. This also
        // sidesteps the cross-origin fetch path on the client (no CORS
        // preflight needed when the link is opened directly).
        String filename = filenameOverride != null && !filenameOverride.isBlank()
            ? filenameOverride
            : (key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key);
        var get = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .responseContentDisposition("attachment; filename=\"" + filename + "\"")
            .build();
        var presigned = presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build()
        );
        return new PresignedUrl(presigned.url().toString(), Instant.now().plus(ttl));
    }
}
