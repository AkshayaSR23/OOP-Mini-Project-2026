package com.dept.budget.service;

import com.dept.budget.exception.InvalidInputException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Turns any table (title + column headers + rows) into an Excel or a PDF file.
 * Used for both the budget proposal and the reports.
 */
public class ExportUtil {

    private ExportUtil() {
        // only static methods, no objects needed
    }

    // ---------- Excel (Apache POI) ----------

    public static byte[] toExcel(String title, List<String> headers, List<List<String>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Report");

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            Cell titleCell = sheet.createRow(0).createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(boldStyle);
            sheet.createRow(1).createCell(0).setCellValue("Department Budget Management System, generated on " + LocalDate.now());

            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(boldStyle);
            }

            int rowNumber = 4;
            for (List<String> rowData : rows) {
                Row row = sheet.createRow(rowNumber++);
                for (int i = 0; i < rowData.size(); i++) {
                    String value = rowData.get(i);
                    Cell cell = row.createCell(i);
                    if (isNumber(value)) {
                        cell.setCellValue(Double.parseDouble(value));   // real numbers, so Excel can add them
                    } else {
                        cell.setCellValue(value);
                    }
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create the Excel file", e);
        }
    }

    // ---------- PDF (OpenPDF) ----------

    public static byte[] toPdf(String title, List<String> headers, List<List<String>> rows) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Wide tables are printed in landscape
            Document document = new Document(headers.size() > 4 ? PageSize.A4.rotate() : PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.lowagie.text.Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph("Department Budget Management System", bodyFont));
            document.add(new Paragraph(title, titleFont));
            document.add(new Paragraph("Generated on " + LocalDate.now(), bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headFont));
                cell.setBackgroundColor(new Color(228, 228, 240));
                cell.setPadding(5);
                table.addCell(cell);
            }
            for (List<String> rowData : rows) {
                for (String value : rowData) {
                    PdfPCell cell = new PdfPCell(new Phrase(value, bodyFont));
                    cell.setPadding(5);
                    if (isNumber(value)) {
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    table.addCell(cell);
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create the PDF file", e);
        }
    }

    // ---------- Helpers ----------

    /** Checks the format asked for: true for "pdf", false for "excel". */
    public static boolean isPdf(String format) {
        if ("pdf".equalsIgnoreCase(format)) return true;
        if ("excel".equalsIgnoreCase(format)) return false;
        throw new InvalidInputException("Choose Excel or PDF for the download");
    }

    /** Sends a file to the browser as a download. */
    public static ResponseEntity<byte[]> fileResponse(byte[] data, String fileName) {
        String type = fileName.endsWith(".pdf")
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(type))
                .body(data);
    }

    private static boolean isNumber(String value) {
        return value != null && value.matches("-?\\d+(\\.\\d+)?");
    }
}
