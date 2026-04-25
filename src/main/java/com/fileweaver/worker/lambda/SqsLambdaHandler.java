package com.fileweaver.worker.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileweaver.Application;
import com.fileweaver.queue.JobMessage;
import com.fileweaver.worker.JobProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Worker Lambda — booted once per container. For each invocation, processes a
 * batch of SQS messages and reports per-message failures so successful messages
 * are acked while failures redeliver via SQS retry policy.
 */
public class SqsLambdaHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final Logger log = LoggerFactory.getLogger(SqsLambdaHandler.class);

    private static volatile ConfigurableApplicationContext context;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static synchronized ConfigurableApplicationContext init() {
        if (context == null) {
            // Worker doesn't serve HTTP. Force non-web mode AND exclude the
            // serverless-web auto-config that gets pulled in transitively by
            // aws-serverless-java-container-springboot3 — it tries to register
            // ServerlessServletWebServerFactory as ApplicationContextAware
            // and ClassCasts on the non-web AnnotationConfigApplicationContext.
            System.setProperty("spring.autoconfigure.exclude",
                "org.springframework.cloud.function.serverless.web.ServerlessAutoConfiguration");
            SpringApplication app = new SpringApplication(Application.class);
            app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
            context = app.run();
        }
        return context;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context awsCtx) {
        ConfigurableApplicationContext ctx = init();
        JobProcessor processor = ctx.getBean(JobProcessor.class);

        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            try {
                JobMessage jm = MAPPER.readValue(msg.getBody(), JobMessage.class);
                processor.process(jm.jobId());
            } catch (Exception e) {
                log.warn("Worker failure for messageId={}, will retry", msg.getMessageId(), e);
                failures.add(SQSBatchResponse.BatchItemFailure.builder()
                    .withItemIdentifier(msg.getMessageId())
                    .build());
            }
        }
        return SQSBatchResponse.builder().withBatchItemFailures(failures).build();
    }
}
