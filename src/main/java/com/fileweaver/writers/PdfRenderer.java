package com.fileweaver.writers;

import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import com.lowagie.text.Document;

public interface PdfRenderer {
    /**
     * The report type this renderer handles.
     * Return null to act as the default/fallback renderer.
     */
    ReportType forType();

    void render(Document doc, ReportData data);
}
