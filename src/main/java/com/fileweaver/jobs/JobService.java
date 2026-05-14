package com.fileweaver.jobs;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static com.fileweaver.jobs.JobStatus.PROCESSING;
import static com.fileweaver.jobs.JobStatus.QUEUED;

@Service
@RequiredArgsConstructor
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository repo;
    private final MongoTemplate mongo;

    @Value("${app.s3.bucket}")
    private String bucket;

    public Job insert(Job job) {
        Instant now = Instant.now();
        if (job.getCreatedAt() == null) job.setCreatedAt(now);
        job.setUpdatedAt(now);
        if (job.getStatus() == null) job.setStatus(QUEUED);
        return repo.insert(job);
    }

    public Optional<Job> findById(String id) {
        return repo.findById(id);
    }

    public Optional<Job> findByIdempotencyKey(String key) {
        return repo.findByIdempotencyKey(key);
    }

    /**
     * Atomic transition QUEUED|PROCESSING -> PROCESSING. Returns the claimed job
     * if the transition happened (or was already PROCESSING), empty if terminal.
     */
    public Optional<Job> tryClaim(String jobId) {
        Query q = Query.query(Criteria.where("_id").is(jobId)
            .and("status").in(QUEUED, PROCESSING));
        Update u = new Update()
            .set("status", PROCESSING)
            .inc("attempts", 1)
            .set("updatedAt", Instant.now());
        var result = mongo.updateFirst(q, u, Job.class);
        if (result.getMatchedCount() == 0) {
            log.info("Job {} not in claimable state", jobId);
            return Optional.empty();
        }
        return repo.findById(jobId);
    }

    public void markCompleted(String jobId, String s3Key, String contentType, long byteSize, String downloadFilename) {
        Instant now = Instant.now();
        Update u = new Update()
            .set("status", JobStatus.COMPLETED)
            .set("s3Key", s3Key)
            .set("s3Bucket", bucket)
            .set("contentType", contentType)
            .set("byteSize", byteSize)
            .set("completedAt", now)
            .set("updatedAt", now);
        if (downloadFilename != null && !downloadFilename.isBlank()) {
            u.set("downloadFilename", downloadFilename);
        }
        mongo.updateFirst(Query.query(Criteria.where("_id").is(jobId)), u, Job.class);
    }

    public void markFailed(String jobId, String errorCode, String message) {
        Update u = new Update()
            .set("status", JobStatus.FAILED)
            .set("error", new Job.JobError(errorCode, message))
            .set("updatedAt", Instant.now());
        mongo.updateFirst(Query.query(Criteria.where("_id").is(jobId)), u, Job.class);
    }
}
