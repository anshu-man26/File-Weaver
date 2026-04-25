package com.fileweaver.reports;

import com.fileweaver.reports.impl.InvoiceReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvoiceReportTest {

    private final InvoiceReport report = new InvoiceReport();

    @Test
    void validates_required_fields() {
        assertThrows(IllegalArgumentException.class, () -> report.validate(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> report.validate(
            Map.of("invoiceNumber", "INV-1")));
    }

    @Test
    void generates_rows_with_line_totals_and_grand_total() {
        Map<String, Object> payload = Map.of(
            "invoiceNumber", "INV-001",
            "customerName", "Acme",
            "items", List.of(
                Map.of("name", "Widget", "qty", 2, "price", 100),
                Map.of("name", "Gadget", "qty", 1, "price", 50)
            )
        );

        ReportData data = report.generate(payload);

        assertThat(data.title()).isEqualTo("Invoice INV-001");
        assertThat(data.headers()).containsExactly("Item", "Qty", "Unit Price", "Total");
        assertThat(data.rows()).hasSize(2);
        assertThat(data.rows().get(0)).containsExactly("Widget", 2, 100, 200.0);
        assertThat(data.metadata()).containsEntry("totalAmount", 250.0);
        assertThat(data.metadata()).containsEntry("customer", "Acme");
    }
}
