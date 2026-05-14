package com.fileweaver.api.dto;

import com.fileweaver.reports.ReportType;
import com.fileweaver.writers.Format;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @NoArgsConstructor
public class CreateReportRequest {

    @NotNull private ReportType          type;
    @NotNull private Format              format;
    @NotNull private Map<String, Object> payload;
             private boolean             forceRegenerate;
}
