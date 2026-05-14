package com.fileweaver.reports.clickandcare;

import com.fileweaver.reports.PayloadValidation;
import com.fileweaver.reports.Report;
import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import org.bson.Document;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AppointmentLogReport implements Report {

    private final ClickAndCareGateway clickandcare;

    public AppointmentLogReport(@Nullable ClickAndCareGateway clickandcare) {
        this.clickandcare = clickandcare;
    }

    @Override
    public ReportType type() { return ReportType.APPOINTMENT_LOG; }

    @Override
    public void validate(Map<String, Object> payload) {
        if (clickandcare == null)
            throw new IllegalStateException("APPOINTMENT_LOG requires CLICKANDCARE_MONGODB_URI");
        PayloadValidation.require(payload, "userId", String.class);
    }

    @Override
    public ReportData generate(Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        List<Document> docs = clickandcare.findAppointmentsByUserId(userId);

        List<List<Object>> rows = new ArrayList<>(docs.size());
        for (Document a : docs) {
            String when      = ClickAndCareGateway.field(a, "slotDate", "") + " " + ClickAndCareGateway.field(a, "slotTime", "");
            Document docData  = a.get("docData",  Document.class);
            Document userData = a.get("userData", Document.class);
            String provider  = ClickAndCareGateway.field(docData,  "name",      "Unknown");
            String speciality= ClickAndCareGateway.field(docData,  "speciality", "");
            String patient   = ClickAndCareGateway.field(userData, "name", "Unknown");
            String status    = deriveStatus(a);
            Object fee       = a.get("amount");
            rows.add(List.of(
                (Object) when,
                patient,
                provider + (speciality.isBlank() ? "" : " (" + speciality + ")"),
                status,
                fee == null ? "" : fee.toString()
            ));
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("count",  docs.size());
        meta.put("userId", userId);

        return new ReportData(
            ReportType.APPOINTMENT_LOG,
            "Appointment Log",
            List.of("When", "Patient", "Provider", "Status", "Fee"),
            rows,
            meta
        );
    }

    private static String deriveStatus(Document a) {
        if (Boolean.TRUE.equals(a.getBoolean("cancelled")))   return "Cancelled";
        if (Boolean.TRUE.equals(a.getBoolean("isCompleted"))) return "Completed";
        if (Boolean.TRUE.equals(a.getBoolean("payment")))     return "Confirmed";
        return "Pending";
    }

}
