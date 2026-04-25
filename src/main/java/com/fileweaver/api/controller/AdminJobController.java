package com.fileweaver.api.controller;

import com.fileweaver.api.dto.JobResponse;
import com.fileweaver.jobs.Job;
import com.fileweaver.jobs.JobRepository;
import com.fileweaver.jobs.JobService;
import com.fileweaver.jobs.JobStatus;
import com.fileweaver.queue.JobMessage;
import com.fileweaver.queue.QueueClient;
import com.fileweaver.reports.ReportType;
import com.fileweaver.writers.Format;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/admin/jobs")
public class AdminJobController {

    private final JobRepository repo;
    private final JobService jobs;
    private final MongoTemplate mongo;
    private final QueueClient queue;
    private final S3Client s3;

    @Value("${app.s3.bucket}")
    private String bucket;

    public AdminJobController(JobRepository repo, JobService jobs, MongoTemplate mongo,
                              QueueClient queue, S3Client s3) {
        this.repo = repo;
        this.jobs = jobs;
        this.mongo = mongo;
        this.queue = queue;
        this.s3 = s3;
    }

    @GetMapping
    public List<JobResponse> list(@RequestParam(required = false) JobStatus status,
                                  @RequestParam(required = false) ReportType type,
                                  @RequestParam(required = false) Format format,
                                  @RequestParam(defaultValue = "50") int limit) {
        Query q = new Query()
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .limit(Math.min(limit, 500));
        if (status != null) q.addCriteria(Criteria.where("status").is(status));
        if (type != null) q.addCriteria(Criteria.where("type").is(type));
        if (format != null) q.addCriteria(Criteria.where("format").is(format));
        return mongo.find(q, Job.class).stream().map(JobResponse::from).toList();
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable String id) {
        return repo.findById(id).map(JobResponse::from).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    @PostMapping("/{id}/retry")
    public JobResponse retry(@PathVariable String id) {
        Job j = repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (j.getStatus() != JobStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Only FAILED jobs can be retried (current: " + j.getStatus() + ")");
        }
        mongo.updateFirst(
            Query.query(Criteria.where("_id").is(id)),
            new Update().set("status", JobStatus.QUEUED)
                .unset("error")
                .set("updatedAt", Instant.now()),
            Job.class
        );
        queue.enqueue(new JobMessage(id));
        return repo.findById(id).map(JobResponse::from).orElseThrow();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Job j = repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (j.getS3Key() != null) {
            try {
                s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(j.getS3Key()).build());
            } catch (Exception ignored) { /* job deletion proceeds either way */ }
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
