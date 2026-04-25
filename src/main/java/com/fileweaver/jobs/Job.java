package com.fileweaver.jobs;

import com.fileweaver.reports.ReportType;
import com.fileweaver.writers.Format;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "jobs")
public class Job {

    @Id
    private String id;

    @Indexed(unique = true)
    private String idempotencyKey;

    private ReportType type;
    private Format format;
    private Map<String, Object> payload;
    private JobStatus status;

    private String s3Key;
    private String s3Bucket;
    private String contentType;
    private Long byteSize;
    /** Optional human-friendly filename for the Content-Disposition header. */
    private String downloadFilename;

    private JobError error;
    private int attempts;

    private RequestedBy requestedBy;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public Job() {}

    public static class RequestedBy {
        private String apiKeyName;
        private String apiKeyId;

        public RequestedBy() {}
        public RequestedBy(String apiKeyName, String apiKeyId) {
            this.apiKeyName = apiKeyName;
            this.apiKeyId = apiKeyId;
        }
        public String getApiKeyName() { return apiKeyName; }
        public void setApiKeyName(String apiKeyName) { this.apiKeyName = apiKeyName; }
        public String getApiKeyId() { return apiKeyId; }
        public void setApiKeyId(String apiKeyId) { this.apiKeyId = apiKeyId; }
    }

    public static class JobError {
        private String code;
        private String message;

        public JobError() {}
        public JobError(String code, String message) {
            this.code = code;
            this.message = message;
        }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public ReportType getType() { return type; }
    public void setType(ReportType type) { this.type = type; }

    public Format getFormat() { return format; }
    public void setFormat(Format format) { this.format = format; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public String getS3Bucket() { return s3Bucket; }
    public void setS3Bucket(String s3Bucket) { this.s3Bucket = s3Bucket; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getByteSize() { return byteSize; }
    public void setByteSize(Long byteSize) { this.byteSize = byteSize; }

    public String getDownloadFilename() { return downloadFilename; }
    public void setDownloadFilename(String downloadFilename) { this.downloadFilename = downloadFilename; }

    public JobError getError() { return error; }
    public void setError(JobError error) { this.error = error; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public RequestedBy getRequestedBy() { return requestedBy; }
    public void setRequestedBy(RequestedBy requestedBy) { this.requestedBy = requestedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
