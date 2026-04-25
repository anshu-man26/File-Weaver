package com.fileweaver.writers;

import com.fileweaver.reports.ReportData;
import com.fileweaver.writers.impl.PdfWriter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PdfWriterTest {

    @Test
    void emits_pdf_magic_bytes_and_correct_content_type() {
        ReportData data = new ReportData(
            "Sample",
            List.of("A", "B"),
            List.of(List.of("x", 1)),
            Map.of("note", "hello")
        );

        WriterOutput out = new PdfWriter().write(data);

        assertThat(out.contentType()).isEqualTo("application/pdf");
        assertThat(out.fileExtension()).isEqualTo("pdf");
        // PDF magic header is "%PDF-"
        assertThat(out.bytes().length).isGreaterThan(4);
        assertThat(new String(out.bytes(), 0, 5)).isEqualTo("%PDF-");
    }
}
