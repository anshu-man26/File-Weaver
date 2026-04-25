package com.fileweaver.writers;

import com.fileweaver.reports.ReportData;

import java.io.IOException;

public interface Writer {

    Format format();

    /**
     * Serialize ReportData into bytes + content type metadata.
     */
    WriterOutput write(ReportData data) throws IOException;
}
