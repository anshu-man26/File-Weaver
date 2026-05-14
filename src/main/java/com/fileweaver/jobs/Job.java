package com.fileweaver.jobs;

import com.fileweaver.reports.ReportType;
import com.fileweaver.writers.Format;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "jobs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Job {

    @Id                          private String              id;
    @Indexed(unique = true)      private String              idempotencyKey;

    private ReportType           type;
    private Format               format;
    private Map<String, Object>  payload;
    private JobStatus            status;

    private String               s3Key;
    private String               s3Bucket;
    private String               contentType;
    private Long                 byteSize;
    private String               downloadFilename;

    private JobError             error;
    private int                  attempts;
    private RequestedBy          requestedBy;

    private Instant              createdAt;
    private Instant              updatedAt;
    private Instant              completedAt;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class RequestedBy {
        private String apiKeyName;
        private String apiKeyId;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class JobError {
        private String code;
        private String message;
    }
}
