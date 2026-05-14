package com.fileweaver.writers.impl;

import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DefaultPdfRenderer extends BasePdfRenderer {

    @Override
    public ReportType forType() { return null; } // fallback for all unregistered types

    @Override
    public void render(Document doc, ReportData data) {
        try {
            doc.add(new Paragraph(data.title() != null ? data.title() : "Report", TITLE));
            doc.add(Chunk.NEWLINE);

            int cols = data.headers().size();
            if (cols > 0) {
                PdfPTable table = new PdfPTable(cols);
                table.setWidthPercentage(100f);

                for (String h : data.headers()) {
                    PdfPCell cell = new PdfPCell(new Phrase(h,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                    cell.setBackgroundColor(BAND_BG);
                    cell.setPadding(8);
                    table.addCell(cell);
                }
                for (List<Object> row : data.rows()) {
                    for (Object v : row) {
                        PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(v), BODY));
                        cell.setPadding(6);
                        table.addCell(cell);
                    }
                }
                doc.add(table);
            }

            if (data.metadata() != null && !data.metadata().isEmpty()) {
                doc.add(Chunk.NEWLINE);
                Paragraph footer = new Paragraph();
                for (Map.Entry<String, Object> e : data.metadata().entrySet()) {
                    footer.add(new Chunk(e.getKey() + ": " + e.getValue() + "\n", META));
                }
                doc.add(footer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
