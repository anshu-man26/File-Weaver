package com.fileweaver.worker;

import com.fileweaver.jobs.Job;
import com.fileweaver.jobs.JobService;
import com.fileweaver.jobs.JobStatus;
import com.fileweaver.reports.Report;
import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportRegistry;
import com.fileweaver.reports.ReportType;
import com.fileweaver.storage.S3Uploader;
import com.fileweaver.writers.Format;
import com.fileweaver.writers.Writer;
import com.fileweaver.writers.WriterFactory;
import com.fileweaver.writers.WriterOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobProcessorTest {

    private final JobService jobs = mock(JobService.class);
    private final ReportRegistry reports = mock(ReportRegistry.class);
    private final WriterFactory writers = mock(WriterFactory.class);
    private final S3Uploader s3 = mock(S3Uploader.class);

    private final JobProcessor processor = new JobProcessor(jobs, reports, writers, s3);

    @Test
    void successful_pipeline_marks_completed_and_uploads_to_s3() {
        Job job = newJob();
        when(jobs.tryClaim("J1")).thenReturn(Optional.of(job));

        Report report = mock(Report.class);
        when(reports.resolve(ReportType.INVOICE)).thenReturn(report);
        when(report.generate(any())).thenReturn(new ReportData("t",
            List.of("a"), List.of(List.of(1)), Map.of()));

        Writer writer = mock(Writer.class);
        when(writers.resolve(Format.CSV)).thenReturn(writer);
        try {
            when(writer.write(any())).thenReturn(new WriterOutput(new byte[]{1, 2, 3}, "text/csv", "csv"));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        processor.process("J1");

        verify(s3).upload(anyString(), any(byte[].class), eq("text/csv"));
        verify(jobs).markCompleted(eq("J1"), anyString(), eq("text/csv"), anyLong(), any());
        verify(jobs, never()).markFailed(any(), any(), any());
    }

    @Test
    void permanent_failure_marks_failed_and_does_not_throw() {
        Job job = newJob();
        when(jobs.tryClaim("J1")).thenReturn(Optional.of(job));

        Report report = mock(Report.class);
        when(reports.resolve(ReportType.INVOICE)).thenReturn(report);
        when(writers.resolve(Format.CSV)).thenReturn(mock(Writer.class));
        // validate() throws IllegalArgumentException → permanent
        org.mockito.Mockito.doThrow(new IllegalArgumentException("bad payload"))
            .when(report).validate(any());

        processor.process("J1");

        verify(jobs).markFailed(eq("J1"), eq("INVALID_PAYLOAD"), anyString());
    }

    @Test
    void retryable_failure_propagates_and_does_not_mark_failed() {
        Job job = newJob();
        when(jobs.tryClaim("J1")).thenReturn(Optional.of(job));

        Report report = mock(Report.class);
        when(reports.resolve(ReportType.INVOICE)).thenReturn(report);
        when(writers.resolve(Format.CSV)).thenReturn(mock(Writer.class));
        org.mockito.Mockito.doThrow(new RetryableException("blip", null))
            .when(report).validate(any());

        try {
            processor.process("J1");
            assert false : "expected RetryableException";
        } catch (RetryableException expected) {
            // good — SQS will redeliver
        }
        verify(jobs, never()).markFailed(any(), any(), any());
        verify(jobs, atLeastOnce()).tryClaim("J1");
    }

    @Test
    void non_claimable_job_is_skipped() {
        when(jobs.tryClaim("J1")).thenReturn(Optional.empty());
        processor.process("J1");
        verify(s3, never()).upload(any(), any(), any());
        verify(jobs, never()).markCompleted(any(), any(), any(), anyLong(), any());
        verify(jobs, never()).markFailed(any(), any(), any());
    }

    private Job newJob() {
        Job j = new Job();
        j.setId("J1");
        j.setType(ReportType.INVOICE);
        j.setFormat(Format.CSV);
        j.setStatus(JobStatus.PROCESSING);
        j.setPayload(Map.of("invoiceNumber", "INV-1"));
        return j;
    }
}
