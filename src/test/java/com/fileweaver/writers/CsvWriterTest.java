package com.fileweaver.writers;

import com.fileweaver.reports.ReportData;
import com.fileweaver.writers.impl.CsvWriter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    @Test
    void writes_header_and_rows() {
        ReportData data = new ReportData(
            "Sample",
            List.of("Col A", "Col B"),
            List.of(List.of("x", 1), List.of("y", 2)),
            Map.of()
        );

        WriterOutput out = new CsvWriter().write(data);

        assertThat(out.contentType()).isEqualTo("text/csv");
        assertThat(out.fileExtension()).isEqualTo("csv");
        String body = new String(out.bytes(), StandardCharsets.UTF_8);
        assertThat(body).contains("\"Col A\"", "\"Col B\"", "\"x\"", "\"1\"", "\"y\"", "\"2\"");
    }
}
