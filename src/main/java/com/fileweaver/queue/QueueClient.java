package com.fileweaver.queue;

public interface QueueClient {
    void enqueue(JobMessage message);
}
