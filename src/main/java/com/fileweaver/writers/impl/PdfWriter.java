package com.fileweaver.writers.impl;

import com.fileweaver.reports.ReportData;
import com.fileweaver.writers.Format;
import com.fileweaver.writers.Writer;
import com.fileweaver.writers.WriterOutput;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Component
public class PdfWriter implements Writer {

    @Override
    public Format format() { return Format.PDF; }

    @Override
    public WriterOutput write(ReportData data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document();
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, baos);
        doc.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        doc.add(new Paragraph(data.title() != null ? data.title() : "Report", titleFont));
        doc.add(Chunk.NEWLINE);

        int cols = data.headers().size();
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100f);

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        for (String h : data.headers()) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(new Color(220, 220, 220));
            cell.setPadding(6);
            table.addCell(cell);
        }
        for (List<Object> row : data.rows()) {
            for (Object v : row) {
                PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(v)));
                cell.setPadding(5);
                table.addCell(cell);
            }
        }
        doc.add(table);

        if (data.metadata() != null && !data.metadata().isEmpty()) {
            doc.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph();
            footer.setAlignment(Element.ALIGN_LEFT);
            for (Map.Entry<String, Object> e : data.metadata().entrySet()) {
                footer.add(new Chunk(e.getKey() + ": " + e.getValue() + "\n"));
            }
            doc.add(footer);
        }
        doc.close();

        return new WriterOutput(baos.toByteArray(), "application/pdf", "pdf");
    }
}
