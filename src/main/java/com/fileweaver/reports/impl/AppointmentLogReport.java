package com.fileweaver.reports.impl;

import com.fileweaver.reports.PayloadValidation;
import com.fileweaver.reports.Report;
import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pulls a user's appointments straight from ClickAndCare's MongoDB
 * (Approach A — fileweaver knows ClickAndCare's appointment schema).
 *
 * Payload: { "userId": "<24-char hex Mongo ObjectId>" }
 *
 * The clickandcare MongoTemplate is optional — fileweaver still boots
 * without CLICKANDCARE_MONGODB_URI set, but APPOINTMENT_LOG requests will
 * fail at validate() with a clear error if it's missing.
 */
@Component
public class AppointmentLogReport implements Report {

    private final MongoTemplate clickandcareMongo;

    public AppointmentLogReport(
            @Qualifier("clickandcareMongoTemplate") @Nullable MongoTemplate clickandcareMongo) {
        this.clickandcareMongo = clickandcareMongo;
    }

    @Override
    public ReportType type() { return ReportType.APPOINTMENT_LOG; }

    @Override
    public void validate(Map<String, Object> payload) {
        if (clickandcareMongo == null) {
            throw new IllegalStateException(
                "APPOINTMENT_LOG requires CLICKANDCARE_MONGODB_URI to be configured");
        }
        PayloadValidation.require(payload, "userId", String.class);
    }

    @Override
    public ReportData generate(Map<String, Object> payload) {
        String userId = (String) payload.get("userId");

        Query q = Query.query(Criteria.where("userId").is(userId))
            .with(Sort.by(Sort.Direction.DESC, "date"));
        List<Document> docs = clickandcareMongo.find(q, Document.class, "appointments");

        List<List<Object>> rows = new ArrayList<>(docs.size());
        for (Document a : docs) {
            String when = nullSafe(a.getString("slotDate")) + " " + nullSafe(a.getString("slotTime"));
            Document docData = a.get("docData", Document.class);
            Document userData = a.get("userData", Document.class);
            String provider = docData != null ? nullSafe(docData.getString("name")) : "Unknown";
            String speciality = docData != null ? nullSafe(docData.getString("speciality")) : "";
            String patient = userData != null ? nullSafe(userData.getString("name")) : "Unknown";
            String status = deriveStatus(a);
            Object fee = a.get("amount");
            rows.add(List.of(
                (Object) when,
                patient,
                provider + (speciality.isBlank() ? "" : " (" + speciality + ")"),
                status,
                fee == null ? "" : fee.toString()
            ));
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("count", docs.size());
        meta.put("userId", userId);

        return new ReportData(
            "Appointment Log",
            List.of("When", "Patient", "Provider", "Status", "Fee"),
            rows,
            meta
        );
    }

    private static String deriveStatus(Document a) {
        if (Boolean.TRUE.equals(a.getBoolean("cancelled"))) return "Cancelled";
        if (Boolean.TRUE.equals(a.getBoolean("isCompleted"))) return "Completed";
        if (Boolean.TRUE.equals(a.getBoolean("payment"))) return "Confirmed";
        return "Pending";
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}
