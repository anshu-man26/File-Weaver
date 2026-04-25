package com.fileweaver.worker;

/**
 * Marker for transient failures that should trigger SQS-level retry rather than
 * a permanent FAILED status. Wrap network blips, S3 5xx, Mongo timeouts, etc.
 */
public class RetryableException extends RuntimeException {
    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
