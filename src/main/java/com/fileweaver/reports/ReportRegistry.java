package com.fileweaver.reports;

import com.fileweaver.reports.exceptions.UnknownReportTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportRegistry {

    private static final Logger log = LoggerFactory.getLogger(ReportRegistry.class);

    private final Map<ReportType, Report> byType;

    public ReportRegistry(List<Report> reports) {
        this.byType = reports.stream()
            .collect(Collectors.toMap(Report::type, Function.identity()));
        log.info("Registered {} report types: {}", byType.size(), byType.keySet());
    }

    public Report resolve(ReportType type) {
        Report r = byType.get(type);
        if (r == null) throw new UnknownReportTypeException(type);
        return r;
    }
}
