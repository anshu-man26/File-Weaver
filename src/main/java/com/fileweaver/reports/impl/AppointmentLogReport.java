package com.fileweaver.reports.impl;

import com.fileweaver.reports.PayloadValidation;
import com.fileweaver.reports.Report;
import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AppointmentLogReport implements Report {

    @Override
    public ReportType type() { return ReportType.APPOINTMENT_LOG; }

    @Override
    public void validate(Map<String, Object> payload) {
        PayloadValidation.require(payload, "appointments", List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ReportData generate(Map<String, Object> payload) {
        List<Map<String, Object>> appts = (List<Map<String, Object>>) payload.get("appointments");
        List<List<Object>> rows = new ArrayList<>(appts.size());
        for (Map<String, Object> a : appts) {
            rows.add(List.of(
                (Object) String.valueOf(a.getOrDefault("when", "")),
                String.valueOf(a.getOrDefault("patient", "")),
                String.valueOf(a.getOrDefault("provider", "")),
                String.valueOf(a.getOrDefault("status", "")),
                String.valueOf(a.getOrDefault("notes", ""))
            ));
        }
        return new ReportData(
            "Appointment Log",
            List.of("When", "Patient", "Provider", "Status", "Notes"),
            rows,
            Map.of("count", appts.size())
        );
    }
}
