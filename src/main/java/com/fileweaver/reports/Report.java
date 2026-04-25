package com.fileweaver.reports;

import java.util.Map;

public interface Report {

    ReportType type();

    /**
     * Throw IllegalArgumentException with a clear message if the payload is malformed for this type.
     */
    void validate(Map<String, Object> payload);

    /**
     * Fetch raw data + transform to the unified ReportData structure.
     */
    ReportData generate(Map<String, Object> payload);
}
