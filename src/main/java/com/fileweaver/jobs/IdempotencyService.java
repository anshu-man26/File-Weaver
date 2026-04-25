package com.fileweaver.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fileweaver.reports.ReportType;
import com.fileweaver.writers.Format;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;

/**
 * Computes a deterministic key for a report request: SHA-256 over
 * "type|format|canonicalJson(payload)|yyyymmdd?".
 *
 * The optional yyyymmdd bucket prevents stale data from being served forever
 * when the underlying source has changed (toggle via app.idempotency.bucket-by-day).
 */
@Service
public class IdempotencyService {

    private final ObjectMapper canonical;
    private final boolean bucketByDay;

    public IdempotencyService(@Value("${app.idempotency.bucket-by-day:true}") boolean bucketByDay) {
        this.canonical = com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();
        this.bucketByDay = bucketByDay;
    }

    public String compute(ReportType type, Format format, Map<String, Object> payload) {
        String canon;
        try {
            canon = canonical.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not canonicalize payload", e);
        }
        StringBuilder material = new StringBuilder()
            .append(type).append('|').append(format).append('|').append(canon);
        if (bucketByDay) {
            material.append('|').append(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        }
        return sha256(material.toString());
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
