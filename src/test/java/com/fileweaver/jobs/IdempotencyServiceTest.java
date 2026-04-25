package com.fileweaver.jobs;

import com.fileweaver.reports.ReportType;
import com.fileweaver.writers.Format;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyServiceTest {

    private final IdempotencyService svc = new IdempotencyService(false);

    @Test
    void identical_payload_produces_same_hash() {
        Map<String, Object> p = Map.of("a", 1, "b", "x");
        assertThat(svc.compute(ReportType.INVOICE, Format.CSV, p))
            .isEqualTo(svc.compute(ReportType.INVOICE, Format.CSV, p));
    }

    @Test
    void key_order_does_not_matter() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("a", 1); a.put("b", 2);
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("b", 2); b.put("a", 1);

        assertThat(svc.compute(ReportType.INVOICE, Format.CSV, a))
            .isEqualTo(svc.compute(ReportType.INVOICE, Format.CSV, b));
    }

    @Test
    void format_differentiates_jobs() {
        Map<String, Object> p = Map.of("a", 1);
        assertThat(svc.compute(ReportType.INVOICE, Format.CSV, p))
            .isNotEqualTo(svc.compute(ReportType.INVOICE, Format.PDF, p));
    }

    @Test
    void type_differentiates_jobs() {
        Map<String, Object> p = Map.of("a", 1);
        assertThat(svc.compute(ReportType.INVOICE, Format.CSV, p))
            .isNotEqualTo(svc.compute(ReportType.TAX_SUMMARY, Format.CSV, p));
    }
}
