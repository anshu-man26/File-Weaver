package com.fileweaver.reports.impl;

import com.fileweaver.reports.PayloadValidation;
import com.fileweaver.reports.Report;
import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TaxSummaryReport implements Report {

    @Override
    public ReportType type() { return ReportType.TAX_SUMMARY; }

    @Override
    public void validate(Map<String, Object> payload) {
        PayloadValidation.require(payload, "period", String.class);
        PayloadValidation.require(payload, "entries", List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ReportData generate(Map<String, Object> payload) {
        String period = (String) payload.get("period");
        List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entries");

        List<List<Object>> rows = new ArrayList<>(entries.size());
        double taxableTotal = 0;
        double taxTotal = 0;
        for (Map<String, Object> e : entries) {
            String category = String.valueOf(e.getOrDefault("category", ""));
            Number taxable = (Number) e.getOrDefault("taxableAmount", 0);
            Number rate = (Number) e.getOrDefault("ratePct", 0);
            double tax = taxable.doubleValue() * rate.doubleValue() / 100.0;
            taxableTotal += taxable.doubleValue();
            taxTotal += tax;
            rows.add(List.of((Object) category, taxable, rate + "%", round2(tax)));
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("period", period);
        meta.put("taxableTotal", round2(taxableTotal));
        meta.put("taxTotal", round2(taxTotal));

        return new ReportData(
            "Tax Summary " + period,
            List.of("Category", "Taxable Amount", "Rate", "Tax"),
            rows,
            meta
        );
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
