package com.fileweaver.api.controller;

import com.fileweaver.api.dto.CreateReportRequest;
import com.fileweaver.api.dto.JobResponse;
import com.fileweaver.auth.ApiKey;
import com.fileweaver.auth.AuthenticatedKey;
import com.fileweaver.jobs.IdempotencyService;
import com.fileweaver.jobs.Job;
import com.fileweaver.jobs.JobService;
import com.fileweaver.jobs.JobStatus;
import com.fileweaver.queue.JobMessage;
import com.fileweaver.queue.QueueClient;
import com.fileweaver.reports.Report;
import com.fileweaver.reports.ReportRegistry;
import com.fileweaver.storage.PresignedUrl;
import com.fileweaver.storage.S3Uploader;
import com.fileweaver.writers.WriterFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final JobService jobs;
    private final ReportRegistry reports;
    private final WriterFactory writers;
    private final IdempotencyService idempotency;
    private final QueueClient queue;
    private final S3Uploader s3;

    @Value("${app.s3.download-url-ttl:PT15M}")
    private Duration downloadTtl;

    @PostMapping
    public ResponseEntity<JobResponse> create(@Valid @RequestBody CreateReportRequest req,
                                              @AuthenticatedKey ApiKey caller) {
        // fail-fast on unknown type / unsupported format / bad payload (returns 400)
        Report report = reports.resolve(req.getType());
        writers.resolve(req.getFormat());
        report.validate(req.getPayload());

        String idemKey;
        if (req.isForceRegenerate()) {
            // Caller explicitly wants a fresh job. Make the idempotency key
            // unique so we never collide with the existing record (and don't
            // pollute future lookups for that payload — those still hash
            // deterministically).
            idemKey = "force-" + UUID.randomUUID() + "-" + HexFormat.of().formatHex(
                Long.toHexString(Instant.now().toEpochMilli()).getBytes());
        } else {
            idemKey = idempotency.compute(req.getType(), req.getFormat(), req.getPayload());
            var existing = jobs.findByIdempotencyKey(idemKey);
            if (existing.isPresent()) {
                Job hit = existing.get();
                return ResponseEntity.ok(decorate(JobResponse.from(hit, true), hit));
            }
        }

        Job job = newJob(req, idemKey, caller);
        try {
            jobs.insert(job);
        } catch (DuplicateKeyException race) {
            // Lost the insert race — read the winner and return idempotent response
            Job winner = jobs.findByIdempotencyKey(idemKey).orElseThrow(() ->
                new IllegalStateException("Duplicate key but no winning job"));
            return ResponseEntity.ok(decorate(JobResponse.from(winner, true), winner));
        }
        queue.enqueue(new JobMessage(job.getId()));

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(JobResponse.from(job, false));
    }

    @GetMapping("/{jobId}")
    public JobResponse get(@PathVariable String jobId, @AuthenticatedKey ApiKey caller) {
        Job job = jobs.findById(jobId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        authorize(job, caller);
        return decorate(JobResponse.from(job), job);
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<Void> download(@PathVariable String jobId, @AuthenticatedKey ApiKey caller) {
        Job job = jobs.findById(jobId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        authorize(job, caller);
        if (job.getStatus() != JobStatus.COMPLETED || job.getS3Key() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job not yet completed");
        }
        PresignedUrl url = s3.presignDownload(job.getS3Key(), downloadTtl, job.getDownloadFilename());
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(url.url()))
            .build();
    }

    private JobResponse decorate(JobResponse base, Job job) {
        if (job.getStatus() == JobStatus.COMPLETED && job.getS3Key() != null) {
            PresignedUrl url = s3.presignDownload(job.getS3Key(), downloadTtl, job.getDownloadFilename());
            return base.withDownload(url.url(), url.expiresAt());
        }
        return base;
    }

    private void authorize(Job job, ApiKey caller) {
        if (caller.getScopes() != null && caller.getScopes().contains("admin")) return;
        var rb = job.getRequestedBy();
        if (rb == null || !caller.getId().equals(rb.getApiKeyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Job belongs to a different API key");
        }
    }

    private Job newJob(CreateReportRequest req, String idemKey, ApiKey caller) {
        Instant now = Instant.now();
        return Job.builder()
            .id(UUID.randomUUID().toString())
            .idempotencyKey(idemKey)
            .type(req.getType())
            .format(req.getFormat())
            .payload(req.getPayload())
            .status(JobStatus.QUEUED)
            .attempts(0)
            .requestedBy(new Job.RequestedBy(caller.getName(), caller.getId()))
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
}
