package com.fileweaver.jobs;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends MongoRepository<Job, String> {
    Optional<Job> findByIdempotencyKey(String idempotencyKey);
}
