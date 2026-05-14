package com.fileweaver.reports.clickandcare;

import com.fileweaver.reports.PayloadValidation;
import com.fileweaver.reports.Report;
import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class UserActivityReport implements Report {

    @Override
    public ReportType type() { return ReportType.USER_ACTIVITY; }

    @Override
    public void validate(Map<String, Object> payload) {
        PayloadValidation.require(payload, "events", List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ReportData generate(Map<String, Object> payload) {
        List<Map<String, Object>> events = (List<Map<String, Object>>) payload.get("events");
        List<List<Object>> rows = new ArrayList<>(events.size());
        for (Map<String, Object> e : events) {
            rows.add(List.of(
                (Object) String.valueOf(e.getOrDefault("timestamp", "")),
                String.valueOf(e.getOrDefault("userId",   "")),
                String.valueOf(e.getOrDefault("action",   "")),
                String.valueOf(e.getOrDefault("metadata", ""))
            ));
        }
        return new ReportData(
            ReportType.USER_ACTIVITY,
            "User Activity",
            List.of("Timestamp", "User", "Action", "Metadata"),
            rows,
            Map.of("eventCount", events.size())
        );
    }
}
