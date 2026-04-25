package com.fileweaver.worker;

import com.fileweaver.queue.JobMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Active when running outside Lambda (local dev or always-on container deployments).
 * In Lambda, the SqsLambdaHandler is the entrypoint instead.
 */
@Component
@Profile("!lambda")
public class JobQueueListener {

    private final JobProcessor processor;

    public JobQueueListener(JobProcessor processor) {
        this.processor = processor;
    }

    @SqsListener("${app.queue.name}")
    public void onMessage(@Payload JobMessage message) {
        processor.process(message.jobId());
    }
}
