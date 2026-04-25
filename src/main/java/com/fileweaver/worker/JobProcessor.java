package com.fileweaver.worker;

import com.fileweaver.jobs.Job;
import com.fileweaver.jobs.JobService;
import com.fileweaver.reports.Report;
import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportRegistry;
import com.fileweaver.reports.exceptions.UnknownReportTypeException;
import com.fileweaver.storage.S3Uploader;
import com.fileweaver.writers.Writer;
import com.fileweaver.writers.WriterFactory;
import com.fileweaver.writers.WriterOutput;
import com.fileweaver.writers.exceptions.UnsupportedFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);

    private final JobService jobs;
    private final ReportRegistry reports;
    private final WriterFactory writers;
    private final S3Uploader s3;

    public JobProcessor(JobService jobs, ReportRegistry reports, WriterFactory writers, S3Uploader s3) {
        this.jobs = jobs;
        this.reports = reports;
        this.writers = writers;
        this.s3 = s3;
    }

    public void process(String jobId) {
        Optional<Job> claimed = jobs.tryClaim(jobId);
        if (claimed.isEmpty()) {
            log.info("Job {} not claimable (already terminal or missing), skipping", jobId);
            return;
        }
        Job job = claimed.get();

        try {
            Report report = reports.resolve(job.getType());
            Writer writer = writers.resolve(job.getFormat());

            // defense in depth — API also validated
            report.validate(job.getPayload());

            ReportData data = report.generate(job.getPayload());
            WriterOutput out = writer.write(data);

            String s3Key = "reports/%s/%s.%s".formatted(
                LocalDate.now(), jobId, out.fileExtension());
            s3.upload(s3Key, out.bytes(), out.contentType());

            // Reports may suggest a human-friendly filename via metadata.
            // Falls back to the s3 key's basename when absent.
            String suggestedFilename = null;
            if (data.metadata() != null) {
                Object fn = data.metadata().get("downloadFilename");
                if (fn instanceof String s && !s.isBlank()) suggestedFilename = s;
            }

            jobs.markCompleted(jobId, s3Key, out.contentType(), out.bytes().length, suggestedFilename);
            log.info("Job {} completed, {} bytes uploaded to {}", jobId, out.bytes().length, s3Key);

        } catch (RetryableException e) {
            log.warn("Job {} transient failure, will retry: {}", jobId, e.getMessage());
            // do not mark FAILED — let SQS redeliver up to maxReceiveCount
            throw e;
        } catch (Exception e) {
            log.error("Job {} permanent failure", jobId, e);
            jobs.markFailed(jobId, errorCode(e), safeMessage(e));
            // swallow — message acks normally so SQS doesn't redeliver
        }
    }

    private static String errorCode(Throwable e) {
        if (e instanceof UnknownReportTypeException) return "UNKNOWN_REPORT_TYPE";
        if (e instanceof UnsupportedFormatException) return "UNSUPPORTED_FORMAT";
        if (e instanceof IllegalArgumentException) return "INVALID_PAYLOAD";
        if (e instanceof S3Exception) return "S3_UPLOAD_FAILED";
        return "INTERNAL_ERROR";
    }

    private static String safeMessage(Throwable e) {
        String m = e.getMessage();
        return (m == null || m.isBlank()) ? e.getClass().getSimpleName() : m;
    }
}
