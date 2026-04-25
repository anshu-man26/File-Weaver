package com.fileweaver.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final MongoTemplate mongo;
    private final S3Client s3;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${spring.application.name:fileweaver}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String version;

    public HealthController(MongoTemplate mongo, S3Client s3) {
        this.mongo = mongo;
        this.s3 = s3;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", appName);
        result.put("version", version);
        result.put("status", "UP");
        result.put("mongo", check(() -> mongo.getDb().getName() != null));
        result.put("s3", check(() -> {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        }));
        return result;
    }

    private static String check(java.util.concurrent.Callable<Boolean> probe) {
        try {
            return Boolean.TRUE.equals(probe.call()) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
