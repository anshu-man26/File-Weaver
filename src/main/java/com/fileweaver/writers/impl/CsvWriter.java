package com.fileweaver.writers.impl;

import com.fileweaver.reports.ReportData;
import com.fileweaver.writers.Format;
import com.fileweaver.writers.Writer;
import com.fileweaver.writers.WriterOutput;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CsvWriter implements Writer {

    @Override
    public Format format() { return Format.CSV; }

    @Override
    public WriterOutput write(ReportData data) {
        StringWriter sw = new StringWriter();
        try (CSVWriter csv = new CSVWriter(sw)) {
            csv.writeNext(data.headers().toArray(String[]::new));
            for (List<Object> row : data.rows()) {
                csv.writeNext(row.stream().map(String::valueOf).toArray(String[]::new));
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("CSV write failed", e);
        }
        return new WriterOutput(
            sw.toString().getBytes(StandardCharsets.UTF_8),
            "text/csv",
            "csv"
        );
    }
}
