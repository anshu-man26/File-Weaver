package com.fileweaver.writers.impl;

import com.fileweaver.writers.PdfRenderer;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;

public abstract class BasePdfRenderer implements PdfRenderer {

    protected static final Color BRAND      = new Color(79,  70,  229);
    protected static final Color INK        = new Color(17,  24,  39);
    protected static final Color SUBINK     = new Color(75,  85,  99);
    protected static final Color MUTED      = new Color(156, 163, 175);
    protected static final Color HAIRLINE   = new Color(229, 231, 235);
    protected static final Color BAND_BG    = new Color(243, 244, 246);
    protected static final Color OK_GREEN   = new Color(16,  185, 129);
    protected static final Color WARN_AMBER = new Color(245, 158, 11);
    protected static final Color CANCEL_RED = new Color(239, 68,  68);

    protected static final Font TITLE      = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   22, INK);
    protected static final Font SECTION    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11, BRAND);
    protected static final Font BODY_BOLD  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   10, INK);
    protected static final Font BODY       = FontFactory.getFont(FontFactory.HELVETICA,        10, INK);
    protected static final Font BODY_SUB   = FontFactory.getFont(FontFactory.HELVETICA,        10, SUBINK);
    protected static final Font META       = FontFactory.getFont(FontFactory.HELVETICA,         9, MUTED);
    protected static final Font TOTAL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   12, INK);

    protected static void addHairline(Document doc, float spaceBefore, float spaceAfter) {
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

    protected static PdfPCell kvLeft(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(6);
        cell.addElement(new Paragraph(label, META));
        cell.addElement(new Paragraph(value, BODY));
        return cell;
    }

    protected static PdfPCell kvRight(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(6);
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        Paragraph l = new Paragraph(label, META);
        l.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        Paragraph v = new Paragraph(value, BODY);
        v.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        cell.addElement(l);
        cell.addElement(v);
        return cell;
    }

    protected static PdfPCell blankCell() {
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    protected static void addTotalsRow(PdfPTable t, String label, String value, boolean bold) {
        Font lf = bold ? TOTAL_BOLD : BODY_SUB;
        Font vf = bold ? TOTAL_BOLD : BODY;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, lf));
        labelCell.setBorder(Rectangle.NO_BORDER);
        if (bold) { labelCell.setBorderColorTop(HAIRLINE); labelCell.setBorderWidthTop(0.5f); }
        labelCell.setPaddingTop(bold ? 8 : 4);
        labelCell.setPaddingBottom(bold ? 8 : 4);
        labelCell.setPaddingLeft(10);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, vf));
        valueCell.setBorder(Rectangle.NO_BORDER);
        if (bold) { valueCell.setBorderColorTop(HAIRLINE); valueCell.setBorderWidthTop(0.5f); }
        valueCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        valueCell.setPaddingTop(bold ? 8 : 4);
        valueCell.setPaddingBottom(bold ? 8 : 4);
        valueCell.setPaddingRight(10);

        t.addCell(labelCell);
        t.addCell(valueCell);
    }

    protected static Color statusColor(String s) {
        if (s == null) return INK;
        String u = s.trim().toLowerCase();
        if (u.equals("paid") || u.equals("completed")) return OK_GREEN;
        if (u.equals("cancelled")) return CANCEL_RED;
        return WARN_AMBER;
    }
}
