package com.fileweaver.api.dto;

import com.fileweaver.reports.ReportType;
import com.fileweaver.writers.Format;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class CreateReportRequest {

    @NotNull
    private ReportType type;

    @NotNull
    private Format format;

    @NotNull
    private Map<String, Object> payload;

    private boolean forceRegenerate;

    public ReportType getType() { return type; }
    public void setType(ReportType type) { this.type = type; }

    public Format getFormat() { return format; }
    public void setFormat(Format format) { this.format = format; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public boolean isForceRegenerate() { return forceRegenerate; }
    public void setForceRegenerate(boolean forceRegenerate) { this.forceRegenerate = forceRegenerate; }
}
