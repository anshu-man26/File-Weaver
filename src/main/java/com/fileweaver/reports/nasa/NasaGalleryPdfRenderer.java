package com.fileweaver.reports.nasa;

import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import com.fileweaver.writers.impl.BasePdfRenderer;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.Map;

@Component
public class NasaGalleryPdfRenderer extends BasePdfRenderer {

    @Override
    public ReportType forType() { return ReportType.NASA_GALLERY; }

    @Override
    @SuppressWarnings("unchecked")
    public void render(Document doc, ReportData data) {
        try {
            Map<String, Object> m     = data.metadata();
            String query              = String.valueOf(m.getOrDefault("query", "")).toUpperCase();
            List<Map<String, Object>> items = (List<Map<String, Object>>) m.get("items");

            // Top accent bar
            PdfPTable topBar = new PdfPTable(1);
            topBar.setWidthPercentage(100f);
            PdfPCell barCell = new PdfPCell(new com.lowagie.text.Phrase(" "));
            barCell.setBackgroundColor(BRAND);
            barCell.setBorder(Rectangle.NO_BORDER);
            barCell.setMinimumHeight(5);
            topBar.addCell(barCell);
            doc.add(topBar);

            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 20)));

            Paragraph nasaLabel = new Paragraph("N A S A",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND));
            nasaLabel.setAlignment(Element.ALIGN_CENTER);
            nasaLabel.setSpacingAfter(2);
            doc.add(nasaLabel);

            Paragraph planetTitle = new Paragraph(query,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 52, INK));
            planetTitle.setAlignment(Element.ALIGN_CENTER);
            planetTitle.setSpacingAfter(4);
            doc.add(planetTitle);

            Paragraph galleryLabel = new Paragraph("IMAGE GALLERY",
                FontFactory.getFont(FontFactory.HELVETICA, 13, SUBINK));
            galleryLabel.setAlignment(Element.ALIGN_CENTER);
            galleryLabel.setSpacingAfter(6);
            doc.add(galleryLabel);

            Paragraph tagline = new Paragraph("Imagery sourced from NASA Image and Video Library",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, MUTED));
            tagline.setAlignment(Element.ALIGN_CENTER);
            tagline.setSpacingAfter(30);
            doc.add(tagline);

            addHairline(doc, 0, 30);

            if (items == null) return;

            for (Map<String, Object> item : items) {
                String imageUrl    = String.valueOf(item.getOrDefault("imageUrl",    ""));
                String title       = String.valueOf(item.getOrDefault("title",       ""));
                String description = String.valueOf(item.getOrDefault("description", ""));
                String date        = String.valueOf(item.getOrDefault("date",        ""));
                String center      = String.valueOf(item.getOrDefault("center",      "NASA"));
                String nasaId      = String.valueOf(item.getOrDefault("nasaId",      ""));
                String keywords    = String.valueOf(item.getOrDefault("keywords",    ""));

                // Image
                if (!imageUrl.isBlank()) {
                    try {
                        com.lowagie.text.Image img =
                            com.lowagie.text.Image.getInstance(java.net.URI.create(imageUrl).toURL());
                        img.setWidthPercentage(100f);
                        img.setSpacingAfter(0);
                        doc.add(img);
                    } catch (Exception ignore) {}
                }

                // Content card with left accent bar
                PdfPTable card = new PdfPTable(new float[]{4f, 96f});
                card.setWidthPercentage(100f);
                card.setSpacingAfter(30);

                PdfPCell accent = new PdfPCell();
                accent.setBackgroundColor(BRAND);
                accent.setBorder(Rectangle.NO_BORDER);
                card.addCell(accent);

                PdfPCell content = new PdfPCell();
                content.setBorder(Rectangle.NO_BORDER);
                content.setBackgroundColor(new Color(243, 244, 246));
                content.setPaddingLeft(16);
                content.setPaddingTop(14);
                content.setPaddingBottom(14);
                content.setPaddingRight(14);

                content.addElement(new Paragraph(title,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, INK)));

                String metaText = date;
                if (!center.isBlank() && !"null".equals(center)) metaText += "  ·  " + center;
                if (!nasaId.isBlank() && !"null".equals(nasaId))  metaText += "  ·  " + nasaId;
                Paragraph metaPara = new Paragraph(metaText,
                    FontFactory.getFont(FontFactory.HELVETICA, 9, SUBINK));
                metaPara.setSpacingBefore(4);
                metaPara.setSpacingAfter(6);
                content.addElement(metaPara);

                if (!keywords.isBlank() && !"null".equals(keywords)) {
                    content.addElement(new Paragraph(keywords,
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, MUTED)));
                }

                if (!description.isBlank() && !"null".equals(description)) {
                    String desc = description.length() > 500
                        ? description.substring(0, 500) + "…" : description;
                    Paragraph descPara = new Paragraph(desc,
                        FontFactory.getFont(FontFactory.HELVETICA, 10, SUBINK));
                    descPara.setSpacingBefore(8);
                    content.addElement(descPara);
                }

                card.addCell(content);
                doc.add(card);
            }

            addHairline(doc, 10, 10);
            Paragraph footer = new Paragraph(
                "NASA Image and Video Library  ·  images.nasa.gov", META);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
