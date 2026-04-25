package com.fileweaver.writers;

import com.fileweaver.reports.ReportData;
import com.fileweaver.writers.impl.ExcelWriter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelWriterTest {

    @Test
    void writes_xlsx_with_header_and_rows() throws IOException {
        ReportData data = new ReportData(
            "Sample / Report",
            List.of("Item", "Qty", "Total"),
            List.of(List.of("Widget", 2, 200.0)),
            Map.of()
        );

        WriterOutput out = new ExcelWriter().write(data);

        assertThat(out.fileExtension()).isEqualTo("xlsx");
        assertThat(out.contentType())
            .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.bytes()))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Item");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Qty");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Widget");
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(2.0);
            assertThat(sheet.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(200.0);
        }
    }
}
