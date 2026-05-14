package com.fileweaver.writers;

import com.fileweaver.reports.ReportType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PdfRendererRegistry {

    private final Map<ReportType, PdfRenderer> renderers = new EnumMap<>(ReportType.class);
    private PdfRenderer fallback;

    public PdfRendererRegistry(List<PdfRenderer> all) {
        for (PdfRenderer r : all) {
            if (r.forType() == null) {
                fallback = r;
            } else {
                renderers.put(r.forType(), r);
            }
        }
    }

    public PdfRenderer resolve(ReportType type) {
        return renderers.getOrDefault(type, fallback);
    }
}
