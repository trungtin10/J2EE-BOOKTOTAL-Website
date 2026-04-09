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

    private static final String[] COLUMNS = {"Mã đơn", "Khách hàng", "Số điện thoại", "Địa chỉ", "Sản phẩm", "Ngày đặt", "Trạng thái", "Thanh toán", "Tổng tiền"};

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
                int colIdx = 0;

                // Mã đơn
                row.createCell(colIdx++).setCellValue("#" + order.getId());

                // Khách hàng
                row.createCell(colIdx++).setCellValue(resolveCustomerLabel(order));

                // Số điện thoại
                String phone = order.getShippingPhone();
                if (phone == null && order.getUser() != null) phone = order.getUser().getPhone();
                row.createCell(colIdx++).setCellValue(phone != null ? phone : "—");

                // Địa chỉ
                String address = order.getShippingAddress();
                row.createCell(colIdx++).setCellValue(address != null ? address : "—");

                // Chi tiết sản phẩm
                StringBuilder productsStr = new StringBuilder();
                if (order.getOrderDetails() != null) {
                    for (com.bookstore.model.OrderDetail detail : order.getOrderDetails()) {
                        if (productsStr.length() > 0) productsStr.append(";\n"); // Use newline for better readability
                        String pName = (detail.getProduct() != null && detail.getProduct().getName() != null) 
                                       ? detail.getProduct().getName() : "Sản phẩm không xác định";
                        productsStr.append("- ").append(pName).append(" (x").append(detail.getQuantity()).append(")");
                    }
                }
                
                Cell productCell = row.createCell(colIdx++);
                productCell.setCellValue(productsStr.length() > 0 ? productsStr.toString() : "—");
                
                // Set wrap text for multi-line products
                CellStyle wrapStyle = workbook.createCellStyle();
                wrapStyle.setWrapText(true);
                productCell.setCellStyle(wrapStyle);

                // Ngày đặt
                Cell dateCell = row.createCell(colIdx++);
                LocalDateTime orderDate = order.getOrderDate();
                if (orderDate != null) {
                    Date excelDate = Date.from(orderDate.atZone(zone).toInstant());
                    dateCell.setCellValue(excelDate);
                    dateCell.setCellStyle(dateStyle);
                } else {
                    dateCell.setBlank();
                }

                // Trạng thái
                row.createCell(colIdx++).setCellValue(order.getStatusVietnamese() != null ? order.getStatusVietnamese() : (order.getStatus() != null ? order.getStatus() : "—"));

                // Thanh toán
                String paymentStr = (order.getPaymentMethod() != null ? order.getPaymentMethod() : "N/A") + " - " +
                                    ("PAID".equals(order.getPaymentStatus()) ? "Đã thanh toán" : "Chưa thanh toán");
                row.createCell(colIdx++).setCellValue(paymentStr);

                // Tổng tiền
                Cell moneyCell = row.createCell(colIdx++);
                double total = order.getFinalTotal() != null ? order.getFinalTotal() : 0.0;
                moneyCell.setCellValue(total);
                moneyCell.setCellStyle(moneyStyle);
            }

            for (int i = 0; i < COLUMNS.length; i++) {
                sheet.autoSizeColumn(i);
                int w = sheet.getColumnWidth(i);
                // Adjust width limits: make product column wider if needed
                if (i == 4) { // Sản phẩm
                    sheet.setColumnWidth(i, Math.min(w + 1024, 100 * 256));
                } else {
                    sheet.setColumnWidth(i, Math.min(w + 1024, 55 * 256));
                }
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
