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
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Component
public class PdfWriter implements Writer {

    // Brand palette
    private static final Color BRAND        = new Color(79, 70, 229);    // indigo-600
    private static final Color INK          = new Color(17, 24, 39);     // gray-900
    private static final Color SUBINK       = new Color(75, 85, 99);     // gray-600
    private static final Color MUTED        = new Color(156, 163, 175);  // gray-400
    private static final Color HAIRLINE     = new Color(229, 231, 235);  // gray-200
    private static final Color BAND_BG      = new Color(243, 244, 246);  // gray-100
    private static final Color OK_GREEN     = new Color(16, 185, 129);   // emerald-500
    private static final Color WARN_AMBER   = new Color(245, 158, 11);   // amber-500
    private static final Color CANCEL_RED   = new Color(239, 68, 68);    // red-500

    private static final Font TITLE      = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, INK);
    private static final Font SECTION    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND);
    private static final Font BODY_BOLD  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, INK);
    private static final Font BODY       = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);
    private static final Font BODY_SUB   = FontFactory.getFont(FontFactory.HELVETICA, 10, SUBINK);
    private static final Font META       = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
    private static final Font TOTAL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, INK);

    @Override
    public Format format() { return Format.PDF; }

    @Override
    public WriterOutput write(ReportData data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, baos);
        doc.open();

        boolean isReceipt = data.metadata() != null
            && "receipt".equals(String.valueOf(data.metadata().get("layout")));
        if (isReceipt) {
            renderReceipt(doc, data);
        } else {
            renderTabular(doc, data);
        }

        doc.close();
        return new WriterOutput(baos.toByteArray(), "application/pdf", "pdf");
    }

    // ──────────────────────────────────────────────────────────────────
    // Receipt layout — invoice-style page with brand header, billed-to
    // / from columns, line-item table, totals, and a payment footer.
    // ──────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void renderReceipt(Document doc, ReportData data) {
        Map<String, Object> m = data.metadata();

        // --- Brand header band ----------------------------------------
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100f);
        try { header.setWidths(new float[]{60f, 40f}); } catch (Exception ignore) {}

        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.addElement(new Paragraph("ClickAndCare",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BRAND)));
        brandCell.addElement(new Paragraph("Online doctor consultations", BODY_SUB));
        header.addCell(brandCell);

        PdfPCell receiptCell = new PdfPCell();
        receiptCell.setBorder(Rectangle.NO_BORDER);
        receiptCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph rl = new Paragraph("RECEIPT",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, INK));
        rl.setAlignment(Element.ALIGN_RIGHT);
        receiptCell.addElement(rl);
        Paragraph rn = new Paragraph(
            String.valueOf(m.getOrDefault("receiptNumber", "")), BODY_SUB);
        rn.setAlignment(Element.ALIGN_RIGHT);
        receiptCell.addElement(rn);
        Paragraph rd = new Paragraph(
            "Issued " + m.getOrDefault("issueDate", ""), BODY_SUB);
        rd.setAlignment(Element.ALIGN_RIGHT);
        receiptCell.addElement(rd);
        header.addCell(receiptCell);
        doc.add(header);

        addHairline(doc, 14, 18);

        // --- Billed To / From columns ---------------------------------
        Map<String, Object> billedTo = (Map<String, Object>) m.getOrDefault("billedTo", Map.of());
        Map<String, Object> from = (Map<String, Object>) m.getOrDefault("from", Map.of());

        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100f);
        parties.addCell(partyCell("BILLED TO", billedTo));
        parties.addCell(partyCell("FROM", from));
        doc.add(parties);

        addHairline(doc, 18, 14);

        // --- Provider details (the appointment itself) ----------------
        Map<String, Object> provider = (Map<String, Object>) m.getOrDefault("provider", Map.of());
        if (!provider.isEmpty()) {
            Paragraph aptHeader = new Paragraph("APPOINTMENT", SECTION);
            aptHeader.setSpacingAfter(6);
            doc.add(aptHeader);

            PdfPTable aptInfo = new PdfPTable(2);
            aptInfo.setWidthPercentage(100f);
            try { aptInfo.setWidths(new float[]{60f, 40f}); } catch (Exception ignore) {}
            aptInfo.addCell(kvLeft("Provider", String.valueOf(provider.getOrDefault("name", ""))));
            aptInfo.addCell(kvRight("Date",
                provider.get("date") + " " + (provider.containsKey("time") ? provider.get("time") : "")));
            String specLine = "";
            if (provider.containsKey("speciality")) specLine += provider.get("speciality");
            if (provider.containsKey("degree")) {
                if (!specLine.isBlank()) specLine += " — ";
                specLine += provider.get("degree");
            }
            if (!specLine.isBlank()) {
                aptInfo.addCell(kvLeft("Qualifications", specLine));
                aptInfo.addCell(blankCell());
            }
            doc.add(aptInfo);
            doc.add(new Paragraph(" ", BODY));
        }

        // --- Line items table ------------------------------------------
        PdfPTable items = new PdfPTable(data.headers().size());
        items.setWidthPercentage(100f);
        try { items.setWidths(new float[]{60f, 10f, 15f, 15f}); } catch (Exception ignore) {}

        for (String h : data.headers()) {
            PdfPCell cell = new PdfPCell(new Phrase(h.toUpperCase(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MUTED)));
            cell.setBackgroundColor(BAND_BG);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingTop(8);
            cell.setPaddingBottom(8);
            cell.setPaddingLeft(10);
            cell.setPaddingRight(10);
            items.addCell(cell);
        }
        for (List<Object> row : data.rows()) {
            for (Object v : row) {
                PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(v), BODY));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setBorderColorBottom(HAIRLINE);
                cell.setBorderWidthBottom(0.5f);
                cell.setPaddingTop(10);
                cell.setPaddingBottom(10);
                cell.setPaddingLeft(10);
                cell.setPaddingRight(10);
                items.addCell(cell);
            }
        }
        doc.add(items);

        // --- Totals ---------------------------------------------------
        Map<String, Object> totals = (Map<String, Object>) m.getOrDefault("totals", Map.of());
        if (!totals.isEmpty()) {
            PdfPTable totalsBox = new PdfPTable(2);
            totalsBox.setWidthPercentage(45f);
            totalsBox.setHorizontalAlignment(Element.ALIGN_RIGHT);
            try { totalsBox.setWidths(new float[]{55f, 45f}); } catch (Exception ignore) {}
            addTotalsRow(totalsBox, "Subtotal", String.valueOf(totals.getOrDefault("subtotal", "")), false);
            if (totals.containsKey("tax")) {
                addTotalsRow(totalsBox, "Tax", String.valueOf(totals.get("tax")), false);
            }
            addTotalsRow(totalsBox, "Total", String.valueOf(totals.getOrDefault("total", "")), true);
            doc.add(totalsBox);
        }

        addHairline(doc, 22, 14);

        // --- Payment block --------------------------------------------
        Map<String, Object> payment = (Map<String, Object>) m.getOrDefault("payment", Map.of());
        if (!payment.isEmpty()) {
            Paragraph payHeader = new Paragraph("PAYMENT", SECTION);
            payHeader.setSpacingAfter(6);
            doc.add(payHeader);

            String status = String.valueOf(payment.getOrDefault("status", ""));
            PdfPTable payTbl = new PdfPTable(2);
            payTbl.setWidthPercentage(100f);
            try { payTbl.setWidths(new float[]{60f, 40f}); } catch (Exception ignore) {}

            // Status (left) — label + value with color-coded value font
            PdfPCell statusCell = new PdfPCell();
            statusCell.setBorder(Rectangle.NO_BORDER);
            statusCell.setPaddingBottom(6);
            statusCell.addElement(new Paragraph("Status", META));
            statusCell.addElement(new Paragraph(status,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, statusColor(status))));
            payTbl.addCell(statusCell);

            payTbl.addCell(kvRight("Method", String.valueOf(payment.getOrDefault("method", ""))));
            if (payment.containsKey("transactionRef")) {
                payTbl.addCell(kvLeft("Transaction reference", String.valueOf(payment.get("transactionRef"))));
                payTbl.addCell(blankCell());
            }
            doc.add(payTbl);
        }

        // --- Footer ---------------------------------------------------
        addHairline(doc, 22, 12);
        Paragraph thanks = new Paragraph("Thank you for choosing ClickAndCare.", BODY);
        thanks.setAlignment(Element.ALIGN_CENTER);
        doc.add(thanks);
        Paragraph foot = new Paragraph(
            "Questions about this receipt? Reach out at support@chikitsalaya.live", META);
        foot.setAlignment(Element.ALIGN_CENTER);
        doc.add(foot);
    }

    private static Color statusColor(String s) {
        if (s == null) return INK;
        String u = s.trim().toLowerCase();
        if (u.equals("paid") || u.equals("completed")) return OK_GREEN;
        if (u.equals("cancelled")) return CANCEL_RED;
        return WARN_AMBER;
    }

    private static PdfPCell partyCell(String header, Map<String, Object> info) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(0);
        cell.setPaddingBottom(0);
        cell.addElement(new Paragraph(header, SECTION));
        if (info != null) {
            for (Map.Entry<String, Object> e : info.entrySet()) {
                String key = e.getKey();
                String val = String.valueOf(e.getValue());
                if ("name".equals(key)) {
                    cell.addElement(new Paragraph(val, BODY_BOLD));
                } else {
                    cell.addElement(new Paragraph(val, BODY_SUB));
                }
            }
        }
        return cell;
    }

    private static PdfPCell kvLeft(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(6);
        cell.addElement(new Paragraph(label, META));
        cell.addElement(new Paragraph(value, BODY));
        return cell;
    }

    private static PdfPCell kvRight(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(6);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph l = new Paragraph(label, META);
        l.setAlignment(Element.ALIGN_RIGHT);
        Paragraph v = new Paragraph(value, BODY);
        v.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(l);
        cell.addElement(v);
        return cell;
    }

    private static PdfPCell blankCell() {
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private static void addTotalsRow(PdfPTable t, String label, String value, boolean bold) {
        Font lf = bold ? TOTAL_BOLD : BODY_SUB;
        Font vf = bold ? TOTAL_BOLD : BODY;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, lf));
        labelCell.setBorder(Rectangle.NO_BORDER);
        if (bold) labelCell.setBorderColorTop(HAIRLINE);
        if (bold) labelCell.setBorderWidthTop(0.5f);
        labelCell.setPaddingTop(bold ? 8 : 4);
        labelCell.setPaddingBottom(bold ? 8 : 4);
        labelCell.setPaddingLeft(10);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, vf));
        valueCell.setBorder(Rectangle.NO_BORDER);
        if (bold) valueCell.setBorderColorTop(HAIRLINE);
        if (bold) valueCell.setBorderWidthTop(0.5f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPaddingTop(bold ? 8 : 4);
        valueCell.setPaddingBottom(bold ? 8 : 4);
        valueCell.setPaddingRight(10);

        t.addCell(labelCell);
        t.addCell(valueCell);
    }

    private static void addHairline(Document doc, float spaceBefore, float spaceAfter) {
        // Use a one-row, one-column table styled as a thin bottom border
        // because OpenPDF doesn't expose a `<hr>`-equivalent directly.
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100f);
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(HAIRLINE);
        cell.setBorderWidthBottom(0.5f);
        cell.setPaddingTop(spaceBefore);
        cell.setPaddingBottom(spaceAfter);
        line.addCell(cell);
        try { doc.add(line); } catch (Exception ignore) {}
    }

    // ──────────────────────────────────────────────────────────────────
    // Default tabular layout for non-receipt reports.
    // ──────────────────────────────────────────────────────────────────
    private void renderTabular(Document doc, ReportData data) {
        doc.add(new Paragraph(data.title() != null ? data.title() : "Report", TITLE));
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
    }
}
