# FileWeaver

Asynchronous report generation microservice. POST a request, get a `jobId`, poll for a pre-signed S3 download URL when ready. Multi-tenant via API keys; multi-format (CSV, PDF, Excel) and arbitrary report types via a registry pattern.

## Stack

- Java 21, Spring Boot 3.3
- MongoDB (Atlas in prod, local container in dev) for jobs + API keys
- AWS SQS (decouples API → worker) with DLQ
- AWS S3 for generated files; pre-signed URLs for download
- Two Lambdas — API Gateway-fronted HTTP, and SQS-triggered worker
- OpenPDF / Apache POI / OpenCSV for file output
- Caffeine for API-key validation cache
- Doppler for secret injection

## Quickstart (local)

```bash
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

Mint an app key:

```bash
curl -X POST http://localhost:8080/admin/api-keys \
  -H "X-Admin-Key: localdev-master" \
  -H "Content-Type: application/json" \
  -d '{"name":"localtest"}'
```

Generate a report:

```bash
curl -X POST http://localhost:8080/reports \
  -H "X-Api-Key: fwk_..." \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INVOICE",
    "format": "CSV",
    "payload": {
      "invoiceNumber": "INV-001",
      "customerName": "Acme",
      "items": [{"name":"Widget","qty":2,"price":100}]
    }
  }'
```

Poll:

```bash
curl http://localhost:8080/reports/<jobId> -H "X-Api-Key: fwk_..."
```

## API surface

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/reports` | `X-Api-Key` | Returns `202 + jobId` for new jobs, `200 + duplicate:true` on idempotent hit |
| GET | `/reports/{jobId}` | `X-Api-Key` | Returns status + (if `COMPLETED`) a fresh 15-min pre-signed URL |
| GET | `/reports/{jobId}/download` | `X-Api-Key` | 302 to pre-signed URL |
| POST | `/admin/api-keys` | `X-Admin-Key` | Plaintext returned **once** |
| GET | `/admin/api-keys` | `X-Admin-Key` | List (no plaintext) |
| PATCH | `/admin/api-keys/{id}/revoke` | `X-Admin-Key` | Deactivate |
| DELETE | `/admin/api-keys/{id}` | `X-Admin-Key` | Hard delete |
| GET | `/admin/jobs` | `X-Admin-Key` | Filterable list |
| POST | `/admin/jobs/{id}/retry` | `X-Admin-Key` | Resets `FAILED → QUEUED` and re-enqueues |
| DELETE | `/admin/jobs/{id}` | `X-Admin-Key` | Removes job + S3 file |
| GET | `/health` | none | `{ status, mongo, s3 }` |

## Adding a new report type

Drop a `@Component` that implements `Report` into `com.fileweaver.reports.impl`. Add the enum value to `ReportType`. Done — Spring's component scan picks it up and `ReportRegistry` includes it without further wiring.

```java
@Component
public class MyReport implements Report {
    public ReportType type() { return ReportType.MY_NEW; }
    public void validate(Map<String, Object> payload) { /* throw IllegalArgumentException if bad */ }
    public ReportData generate(Map<String, Object> payload) { return new ReportData(...); }
}
```

Same pattern for new formats — implement `Writer`.

## Deployment

```bash
./gradlew shadowJar
doppler run --config=fileweaver-be/production -- serverless deploy --stage production
```

Or run the GitHub Actions workflow `Release` (manual `workflow_dispatch`).

### Required env

| Var | Notes |
|---|---|
| `MONGODB_URI` | Atlas connection string |
| `REPORT_ADMIN_KEY` | Master key for `/admin/*` |
| `AWS_REGION` | e.g. `ap-south-1` |

S3 bucket and SQS queue are provisioned by `serverless.yml`.

## Project layout

See `report-service-blueprint.md` (the design doc) for the full architectural rationale.

```
src/main/java/com/fileweaver/
├── api/             — controllers, DTOs, exception handler, Lambda HTTP entrypoint
├── auth/            — ApiKey document/repo/service/filter, admin key filter, generator
├── jobs/            — Job document/repo, JobService, JobStatus, IdempotencyService
├── reports/         — Report SPI + ReportType + ReportData + registry + impls
├── writers/         — Writer SPI + Format + WriterOutput + factory + impls (CSV/PDF/Excel)
├── worker/          — JobProcessor, JobQueueListener, RetryableException, Lambda SQS handler
├── storage/         — S3Uploader (put + presign)
├── queue/           — QueueClient + SqsQueueClient + JobMessage
└── config/          — AwsConfig, WebMvcConfig, AsyncConfig
```

## Status semantics

```
QUEUED → PROCESSING → COMPLETED
                   ↘ FAILED  (admin /retry resets to QUEUED)
```

Worker `tryClaim` is an atomic Mongo update — duplicate SQS deliveries do not re-run a completed job.

## Idempotency

`idempotencyKey = sha256(type | format | sortedJson(payload) | yyyymmdd)`

The `yyyymmdd` bucket prevents stale data from being served forever when the source has changed. Set `app.idempotency.bucket-by-day=false` to disable, or pass `forceRegenerate: true` in the request body to bypass dedup once.
