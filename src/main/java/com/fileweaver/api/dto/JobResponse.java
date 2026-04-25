package com.fileweaver.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fileweaver.jobs.Job;
import com.fileweaver.jobs.JobStatus;
import com.fileweaver.reports.ReportType;
import com.fileweaver.writers.Format;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobResponse(
    String jobId,
    ReportType type,
    Format format,
    JobStatus status,
    Instant createdAt,
    Instant completedAt,
    String downloadUrl,
    Instant downloadUrlExpiresAt,
    String contentType,
    Long byteSize,
    ErrorBody error,
    Boolean duplicate
) {
    public record ErrorBody(String code, String message) {}

    public static JobResponse from(Job job) {
        return from(job, null);
    }

    public static JobResponse from(Job job, Boolean duplicate) {
        ErrorBody err = null;
        if (job.getError() != null) {
            err = new ErrorBody(job.getError().getCode(), job.getError().getMessage());
        }
        return new JobResponse(
            job.getId(),
            job.getType(),
            job.getFormat(),
            job.getStatus(),
            job.getCreatedAt(),
            job.getCompletedAt(),
            null, null,
            job.getContentType(),
            job.getByteSize(),
            err,
            duplicate
        );
    }

    public JobResponse withDownload(String url, Instant expiresAt) {
        return new JobResponse(
            jobId, type, format, status, createdAt, completedAt,
            url, expiresAt, contentType, byteSize, error, duplicate
        );
    }
}
