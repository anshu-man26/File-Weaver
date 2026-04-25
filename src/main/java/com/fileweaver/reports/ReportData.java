package com.fileweaver.reports;

import java.util.List;
import java.util.Map;

public record ReportData(
    String title,
    List<String> headers,
    List<List<Object>> rows,
    Map<String, Object> metadata
) {}
