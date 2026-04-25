package com.fileweaver.writers.impl;

import com.fileweaver.reports.ReportData;
import com.fileweaver.writers.Format;
import com.fileweaver.writers.Writer;
import com.fileweaver.writers.WriterOutput;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ExcelWriter implements Writer {

    private static final Pattern INVALID_SHEET_CHARS = Pattern.compile("[\\\\/\\?\\*\\[\\]:]");

    @Override
    public Format format() { return Format.EXCEL; }

    @Override
    public WriterOutput write(ReportData data) throws IOException {
        try (Workbook wb = new SXSSFWorkbook(100)) {
            Sheet sheet = wb.createSheet(safeSheetName(data.title()));
            CellStyle headerStyle = headerStyle(wb);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < data.headers().size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(data.headers().get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int r = 0; r < data.rows().size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<Object> rowData = data.rows().get(r);
                for (int c = 0; c < rowData.size(); c++) {
                    setCellValue(row.createCell(c), rowData.get(c));
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            ((SXSSFWorkbook) wb).dispose();
            return new WriterOutput(
                baos.toByteArray(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "xlsx"
            );
        }
    }

    private static String safeSheetName(String title) {
        if (title == null || title.isBlank()) return "Report";
        String cleaned = INVALID_SHEET_CHARS.matcher(title).replaceAll(" ").trim();
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    private static CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static void setCellValue(Cell cell, Object v) {
        if (v == null) {
            cell.setBlank();
        } else if (v instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (v instanceof Boolean b) {
            cell.setCellValue(b);
        } else if (v instanceof Date d) {
            cell.setCellValue(d);
        } else if (v instanceof LocalDate ld) {
            cell.setCellValue(ld);
        } else if (v instanceof LocalDateTime ldt) {
            cell.setCellValue(ldt);
        } else if (v instanceof Instant i) {
            cell.setCellValue(Date.from(i));
        } else {
            cell.setCellValue(String.valueOf(v));
        }
    }
}
