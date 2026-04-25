package com.fileweaver.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class SqsQueueClient implements QueueClient {

    private static final Logger log = LoggerFactory.getLogger(SqsQueueClient.class);

    private final SqsClient sqs;
    private final ObjectMapper mapper;

    @Value("${app.queue.name}")
    private String queueName;

    private volatile String queueUrl;

    public SqsQueueClient(SqsClient sqs, ObjectMapper mapper) {
        this.sqs = sqs;
        this.mapper = mapper;
    }

    @Override
    public void enqueue(JobMessage message) {
        String body;
        try {
            body = mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize JobMessage", e);
        }
        sqs.sendMessage(SendMessageRequest.builder()
            .queueUrl(resolveQueueUrl())
            .messageBody(body)
            .build());
        log.debug("Enqueued jobId={}", message.jobId());
    }

    private String resolveQueueUrl() {
        String url = queueUrl;
        if (url == null) {
            synchronized (this) {
                if (queueUrl == null) {
                    queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName).build()).queueUrl();
                }
                url = queueUrl;
            }
        }
        return url;
    }
}
