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
public class InvoiceReport implements Report {

    @Override
    public ReportType type() { return ReportType.INVOICE; }

    @Override
    public void validate(Map<String, Object> payload) {
        PayloadValidation.require(payload, "invoiceNumber", String.class);
        PayloadValidation.require(payload, "customerName", String.class);
        PayloadValidation.require(payload, "items", List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ReportData generate(Map<String, Object> payload) {
        String invoiceNumber = (String) payload.get("invoiceNumber");
        String customerName = (String) payload.get("customerName");
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        List<List<Object>> rows = new ArrayList<>(items.size());
        double total = 0;
        for (Map<String, Object> item : items) {
            String name = String.valueOf(item.get("name"));
            Number qty = (Number) item.getOrDefault("qty", 0);
            Number price = (Number) item.getOrDefault("price", 0);
            double line = qty.doubleValue() * price.doubleValue();
            total += line;
            rows.add(List.of((Object) name, qty, price, line));
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("totalAmount", total);
        meta.put("customer", customerName);

        return new ReportData(
            "Invoice " + invoiceNumber,
            List.of("Item", "Qty", "Unit Price", "Total"),
            rows,
            meta
        );
    }
}
