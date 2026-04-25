package com.fileweaver.e2e;

import com.fileweaver.auth.ApiKey;
import com.fileweaver.auth.ApiKeyGenerator;
import com.fileweaver.auth.ApiKeyRepository;
import com.fileweaver.jobs.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end happy path: POST /reports → SQS → worker → S3 upload → COMPLETED.
 * Skipped automatically when Docker is not available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
class EndToEndIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7"));

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3"))
        .withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.SQS);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        r.add("app.aws.endpoint", () -> localstack.getEndpoint().toString());
        r.add("app.aws.region", localstack::getRegion);
        r.add("app.aws.access-key", localstack::getAccessKey);
        r.add("app.aws.secret-key", localstack::getSecretKey);
        r.add("app.s3.bucket", () -> "fileweaver-test");
        r.add("app.queue.name", () -> "fileweaver-test-jobs");
        r.add("app.auth.admin-key", () -> "test-admin");
    }

    @LocalServerPort int port;

    @Autowired ApiKeyRepository apiKeyRepo;

    private String plaintextKey;

    @BeforeEach
    void setup() throws Exception {
        // bootstrap S3 bucket + SQS queue inside LocalStack
        localstack.execInContainer("awslocal", "s3", "mb", "s3://fileweaver-test");
        localstack.execInContainer("awslocal", "sqs", "create-queue",
            "--queue-name", "fileweaver-test-jobs");

        // mint an API key directly in the repo
        plaintextKey = ApiKeyGenerator.generate();
        ApiKey k = new ApiKey();
        k.setName("test");
        k.setKeyHash(ApiKeyGenerator.hash(plaintextKey));
        k.setPrefix(ApiKeyGenerator.prefix(plaintextKey));
        k.setActive(true);
        k.setScopes(List.of("reports:create", "reports:read"));
        k.setCreatedAt(Instant.now());
        apiKeyRepo.save(k);
    }

    @Test
    void post_then_poll_eventually_completes() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        String body = """
            {
              "type": "INVOICE",
              "format": "CSV",
              "payload": {
                "invoiceNumber": "INV-IT",
                "customerName": "TestCo",
                "items": [{"name":"X","qty":1,"price":10}]
              }
            }
            """;

        HttpResponse<String> create = http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/reports"))
                .header("X-Api-Key", plaintextKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertThat(create.statusCode()).isEqualTo(202);

        String jobId = extractJobId(create.body());
        assertThat(jobId).isNotBlank();

        // Poll up to 30s for COMPLETED.
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        String last = null;
        while (Instant.now().isBefore(deadline)) {
            HttpResponse<String> get = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/reports/" + jobId))
                    .header("X-Api-Key", plaintextKey)
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            last = get.body();
            if (last.contains("\"status\":\"" + JobStatus.COMPLETED + "\"")) {
                assertThat(last).contains("downloadUrl");
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Job did not complete in time. Last body=" + last);
    }

    private static String extractJobId(String json) {
        // tiny extractor — keeps the test free of jackson
        int i = json.indexOf("\"jobId\"");
        if (i < 0) return null;
        int q1 = json.indexOf('"', i + 8);
        int q2 = json.indexOf('"', q1 + 1);
        return json.substring(q1 + 1, q2);
    }
}
