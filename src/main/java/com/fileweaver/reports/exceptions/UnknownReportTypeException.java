package com.fileweaver.reports.exceptions;

import com.fileweaver.reports.ReportType;

public class UnknownReportTypeException extends RuntimeException {
    public UnknownReportTypeException(ReportType type) {
        super("Unknown report type: " + type);
    }
    public UnknownReportTypeException(String raw) {
        super("Unknown report type: " + raw);
    }
}
