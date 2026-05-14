package com.fileweaver.reports.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileweaver.reports.PayloadValidation;
import com.fileweaver.reports.Report;
import com.fileweaver.reports.ReportData;
import com.fileweaver.reports.ReportType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NasaGalleryReport implements Report {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ReportType type() { return ReportType.NASA_GALLERY; }

    @Override
    public void validate(Map<String, Object> payload) {
        PayloadValidation.require(payload, "query", String.class);
    }

    @Override
    public ReportData generate(Map<String, Object> payload) {
        String query = (String) payload.get("query");
        int limit = payload.containsKey("limit")
            ? Math.min(((Number) payload.get("limit")).intValue(), 10)
            : 5;

        String url = "https://images-api.nasa.gov/search?q="
            + URLEncoder.encode(query, StandardCharsets.UTF_8)
            + "&media_type=image&page_size=" + limit;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode items = mapper.readTree(response.body()).path("collection").path("items");

            List<Map<String, Object>> gallery = new ArrayList<>();
            for (JsonNode item : items) {
                JsonNode data = item.path("data").path(0);
                JsonNode links = item.path("links").path(0);

                List<String> keywords = new ArrayList<>();
                for (JsonNode k : data.path("keywords")) keywords.add(k.asText());

                String dateRaw = data.path("date_created").asText("");
                String date = dateRaw.length() >= 10 ? dateRaw.substring(0, 10) : dateRaw;

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("title", data.path("title").asText("Unknown"));
                entry.put("description", data.path("description").asText(""));
                entry.put("date", date);
                entry.put("center", data.path("center").asText("NASA"));
                entry.put("imageUrl", links.path("href").asText(""));
                entry.put("keywords", String.join(", ", keywords));
                gallery.add(entry);
            }

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("layout", "nasa_gallery");
            meta.put("query", query);
            meta.put("items", gallery);
            meta.put("downloadFilename", "nasa-" + query.replace(" ", "-") + ".pdf");

            return new ReportData("NASA Gallery — " + query, List.of(), List.of(), meta);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch NASA images: " + e.getMessage(), e);
        }
    }
}
