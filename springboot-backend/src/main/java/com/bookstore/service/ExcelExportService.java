package com.bookstore.service;

import com.bookstore.model.Order;
import com.bookstore.model.User;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Xuất đơn hàng ra .xlsx (Office Open XML) — tương thích Excel, LibreOffice Calc, Google Sheets.
 */
@Service
public class ExcelExportService {

    private static final String[] COLUMNS = {"Mã đơn", "Khách hàng", "Ngày đặt", "Tổng tiền"};

    public ByteArrayInputStream exportOrdersToExcel(List<Order> orders) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CreationHelper helper = workbook.getCreationHelper();
            Sheet sheet = workbook.createSheet("Đơn hàng");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0"));

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < COLUMNS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(COLUMNS[col]);
                cell.setCellStyle(headerStyle);
            }

            ZoneId zone = ZoneId.systemDefault();
            int rowIdx = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue("#" + order.getId());

                row.createCell(1).setCellValue(resolveCustomerLabel(order));

                Cell dateCell = row.createCell(2);
                LocalDateTime orderDate = order.getOrderDate();
                if (orderDate != null) {
                    Date excelDate = Date.from(orderDate.atZone(zone).toInstant());
                    dateCell.setCellValue(excelDate);
                    dateCell.setCellStyle(dateStyle);
                } else {
                    dateCell.setBlank();
                }

                Cell moneyCell = row.createCell(3);
                double total = order.getFinalTotal() != null ? order.getFinalTotal() : 0.0;
                moneyCell.setCellValue(total);
                moneyCell.setCellStyle(moneyStyle);
            }

            for (int i = 0; i < COLUMNS.length; i++) {
                sheet.autoSizeColumn(i);
                int w = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(w + 1024, 55 * 256));
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private static String resolveCustomerLabel(Order order) {
        String s = firstNonBlank(order.getShippingName());
        if (s != null) return s;
        User u = order.getUser();
        if (u != null) {
            s = firstNonBlank(u.getFullName());
            if (s != null) return s;
            s = firstNonBlank(u.getUsername());
            if (s != null) return s;
        }
        return "—";
    }

    private static String firstNonBlank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
