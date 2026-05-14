package com.fileweaver.writers.impl;

import com.fileweaver.reports.ReportData;
import com.fileweaver.writers.Format;
import com.fileweaver.writers.PdfRendererRegistry;
import com.fileweaver.writers.Writer;
import com.fileweaver.writers.WriterOutput;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class PdfWriter implements Writer {

    private final PdfRendererRegistry registry;

    public PdfWriter(PdfRendererRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Format format() { return Format.PDF; }

    @Override
    public WriterOutput write(ReportData data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, baos);
        doc.open();
        registry.resolve(data.type()).render(doc, data);
        doc.close();
        return new WriterOutput(baos.toByteArray(), "application/pdf", "pdf");
    }
}
