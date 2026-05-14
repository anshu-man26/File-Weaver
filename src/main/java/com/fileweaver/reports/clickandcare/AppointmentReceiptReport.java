package com.fileweaver.reports.clickandcare;

import com.fileweaver.reports.PayloadValidation;
import com.fileweaver.reports.Report;
import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import org.bson.Document;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AppointmentReceiptReport implements Report {

    private static final DateTimeFormatter ISSUE_DATE_FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.of("Asia/Kolkata"));

    private final ClickAndCareGateway clickandcare;

    public AppointmentReceiptReport(@Nullable ClickAndCareGateway clickandcare) {
        this.clickandcare = clickandcare;
    }

    @Override
    public ReportType type() { return ReportType.APPOINTMENT_RECEIPT; }

    @Override
    public void validate(Map<String, Object> payload) {
        if (clickandcare == null)
            throw new IllegalStateException("APPOINTMENT_RECEIPT requires CLICKANDCARE_MONGODB_URI");
        PayloadValidation.require(payload, "appointmentId", String.class);
    }

    @Override
    public ReportData generate(Map<String, Object> payload) {
        String appointmentId = (String) payload.get("appointmentId");
        Document appt = clickandcare.findAppointmentById(appointmentId);
        if (appt == null)
            throw new IllegalArgumentException("Appointment not found: " + appointmentId);

        Document docData  = appt.get("docData",  Document.class);
        Document userData = appt.get("userData", Document.class);

        String docName    = ClickAndCareGateway.field(docData, "name",      "Unknown Doctor");
        String docDegree  = ClickAndCareGateway.field(docData, "degree",    "");
        String docSpec    = ClickAndCareGateway.field(docData, "speciality","");
        String docFees    = appt.get("amount") == null ? "0" : appt.get("amount").toString();

        String patientName  = ClickAndCareGateway.field(userData, "name",  "Patient");
        String patientEmail = ClickAndCareGateway.field(userData, "email", "");
        String patientPhone = ClickAndCareGateway.field(userData, "phone", "");

        String slotDate = ClickAndCareGateway.field(appt, "slotDate", "");
        String slotTime = ClickAndCareGateway.field(appt, "slotTime", "");

        String description = "Doctor consultation with " + docName
            + (docSpec.isBlank() ? "" : " (" + docSpec + ")")
            + " on " + slotDate + " at " + slotTime;

        List<List<Object>> rows = List.of(List.of(
            (Object) description, "1", "INR " + docFees, "INR " + docFees));

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("receiptNumber", "RCT-" + appointmentId.substring(Math.max(0, appointmentId.length() - 8)).toUpperCase());
        meta.put("appointmentId", appointmentId);
        Number issuedAt = appt.get("date", Number.class);
        meta.put("issueDate", ISSUE_DATE_FMT.format(
            issuedAt != null ? Instant.ofEpochMilli(issuedAt.longValue()) : Instant.now()));

        String slugDate = ClickAndCareGateway.field(appt, "slotDate", "").replaceAll("[^A-Za-z0-9]+", "-");
        meta.put("downloadFilename", slug(patientName)
            + "_" + slug(docName.startsWith("Dr") ? docName : "Dr-" + docName)
            + (slugDate.isBlank() ? "" : "_" + slugDate) + ".pdf");

        Map<String, Object> billedTo = new LinkedHashMap<>();
        billedTo.put("name", patientName);
        if (!patientEmail.isBlank()) billedTo.put("email", patientEmail);
        if (!patientPhone.isBlank()) billedTo.put("phone", patientPhone);
        meta.put("billedTo", billedTo);

        Map<String, Object> from = new LinkedHashMap<>();
        from.put("name", "ClickAndCare");
        from.put("website", "chikitsalaya.live");
        from.put("support", "support@chikitsalaya.live");
        meta.put("from", from);

        Map<String, Object> provider = new LinkedHashMap<>();
        provider.put("name", docName);
        if (!docDegree.isBlank()) provider.put("degree", docDegree);
        if (!docSpec.isBlank())   provider.put("speciality", docSpec);
        provider.put("date", slotDate);
        provider.put("time", slotTime);
        meta.put("provider", provider);

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("subtotal", "INR " + docFees);
        totals.put("tax",      "INR 0");
        totals.put("total",    "INR " + docFees);
        meta.put("totals", totals);

        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("status",         deriveStatus(appt));
        payment.put("method",         "Stripe (Card)");
        payment.put("transactionRef", appointmentId);
        meta.put("payment", payment);

        return new ReportData(
            ReportType.APPOINTMENT_RECEIPT,
            "Receipt — " + meta.get("receiptNumber"),
            List.of("Description", "Qty", "Unit Price", "Total"),
            rows,
            meta
        );
    }

    private static String deriveStatus(Document a) {
        if (Boolean.TRUE.equals(a.getBoolean("cancelled")))  return "Cancelled";
        if (Boolean.TRUE.equals(a.getBoolean("isCompleted"))) return "Completed";
        if (Boolean.TRUE.equals(a.getBoolean("payment")))    return "Paid";
        return "Pending";
    }

    private static String slug(String s) {
        if (s == null || s.isBlank()) return "x";
        return s.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }
}
