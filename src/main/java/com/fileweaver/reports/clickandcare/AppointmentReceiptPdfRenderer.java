package com.fileweaver.reports.clickandcare;

import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import com.fileweaver.writers.impl.BasePdfRenderer;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AppointmentReceiptPdfRenderer extends BasePdfRenderer {

    @Override
    public ReportType forType() { return ReportType.APPOINTMENT_RECEIPT; }

    @Override
    @SuppressWarnings("unchecked")
    public void render(Document doc, ReportData data) {
        try {
            Map<String, Object> m = data.metadata();

            // Brand header
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100f);
            header.setWidths(new float[]{60f, 40f});

            PdfPCell brandCell = new PdfPCell();
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.addElement(new Paragraph("ClickAndCare",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BRAND)));
            brandCell.addElement(new Paragraph("Online doctor consultations", BODY_SUB));
            header.addCell(brandCell);

            PdfPCell receiptCell = new PdfPCell();
            receiptCell.setBorder(Rectangle.NO_BORDER);
            receiptCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph rl = new Paragraph("RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, INK));
            rl.setAlignment(Element.ALIGN_RIGHT);
            receiptCell.addElement(rl);
            Paragraph rn = new Paragraph(String.valueOf(m.getOrDefault("receiptNumber", "")), BODY_SUB);
            rn.setAlignment(Element.ALIGN_RIGHT);
            receiptCell.addElement(rn);
            Paragraph rd = new Paragraph("Issued " + m.getOrDefault("issueDate", ""), BODY_SUB);
            rd.setAlignment(Element.ALIGN_RIGHT);
            receiptCell.addElement(rd);
            header.addCell(receiptCell);
            doc.add(header);

            addHairline(doc, 14, 18);

            // Billed To / From
            Map<String, Object> billedTo = (Map<String, Object>) m.getOrDefault("billedTo", Map.of());
            Map<String, Object> from     = (Map<String, Object>) m.getOrDefault("from",     Map.of());
            PdfPTable parties = new PdfPTable(2);
            parties.setWidthPercentage(100f);
            parties.addCell(partyCell("BILLED TO", billedTo));
            parties.addCell(partyCell("FROM",      from));
            doc.add(parties);

            addHairline(doc, 18, 14);

            // Provider / appointment
            Map<String, Object> provider = (Map<String, Object>) m.getOrDefault("provider", Map.of());
            if (!provider.isEmpty()) {
                Paragraph aptHeader = new Paragraph("APPOINTMENT", SECTION);
                aptHeader.setSpacingAfter(6);
                doc.add(aptHeader);

                PdfPTable aptInfo = new PdfPTable(2);
                aptInfo.setWidthPercentage(100f);
                aptInfo.setWidths(new float[]{60f, 40f});
                aptInfo.addCell(kvLeft("Provider", String.valueOf(provider.getOrDefault("name", ""))));
                aptInfo.addCell(kvRight("Date",
                    provider.get("date") + " " + provider.getOrDefault("time", "")));
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

            // Line items
            PdfPTable items = new PdfPTable(data.headers().size());
            items.setWidthPercentage(100f);
            items.setWidths(new float[]{60f, 10f, 15f, 15f});
            for (String h : data.headers()) {
                PdfPCell cell = new PdfPCell(new Phrase(h.toUpperCase(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MUTED)));
                cell.setBackgroundColor(BAND_BG);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPaddingTop(8); cell.setPaddingBottom(8);
                cell.setPaddingLeft(10); cell.setPaddingRight(10);
                items.addCell(cell);
            }
            for (List<Object> row : data.rows()) {
                for (Object v : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(v), BODY));
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setBorderColorBottom(HAIRLINE);
                    cell.setBorderWidthBottom(0.5f);
                    cell.setPaddingTop(10); cell.setPaddingBottom(10);
                    cell.setPaddingLeft(10); cell.setPaddingRight(10);
                    items.addCell(cell);
                }
            }
            doc.add(items);

            // Totals
            Map<String, Object> totals = (Map<String, Object>) m.getOrDefault("totals", Map.of());
            if (!totals.isEmpty()) {
                PdfPTable totalsBox = new PdfPTable(2);
                totalsBox.setWidthPercentage(45f);
                totalsBox.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalsBox.setWidths(new float[]{55f, 45f});
                addTotalsRow(totalsBox, "Subtotal", String.valueOf(totals.getOrDefault("subtotal", "")), false);
                if (totals.containsKey("tax"))
                    addTotalsRow(totalsBox, "Tax", String.valueOf(totals.get("tax")), false);
                addTotalsRow(totalsBox, "Total", String.valueOf(totals.getOrDefault("total", "")), true);
                doc.add(totalsBox);
            }

            addHairline(doc, 22, 14);

            // Payment
            Map<String, Object> payment = (Map<String, Object>) m.getOrDefault("payment", Map.of());
            if (!payment.isEmpty()) {
                Paragraph payHeader = new Paragraph("PAYMENT", SECTION);
                payHeader.setSpacingAfter(6);
                doc.add(payHeader);

                String status = String.valueOf(payment.getOrDefault("status", ""));
                PdfPTable payTbl = new PdfPTable(2);
                payTbl.setWidthPercentage(100f);
                payTbl.setWidths(new float[]{60f, 40f});

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

            addHairline(doc, 22, 12);

            Paragraph thanks = new Paragraph("Thank you for choosing ClickAndCare.", BODY);
            thanks.setAlignment(Element.ALIGN_CENTER);
            doc.add(thanks);
            Paragraph foot = new Paragraph(
                "Questions about this receipt? Reach out at support@chikitsalaya.live", META);
            foot.setAlignment(Element.ALIGN_CENTER);
            doc.add(foot);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PdfPCell partyCell(String header, Map<String, Object> info) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(new Paragraph(header, SECTION));
        if (info != null) {
            for (Map.Entry<String, Object> e : info.entrySet()) {
                String key = e.getKey();
                String val = String.valueOf(e.getValue());
                cell.addElement(new Paragraph(val, "name".equals(key) ? BODY_BOLD : BODY_SUB));
            }
        }
        return cell;
    }
}
