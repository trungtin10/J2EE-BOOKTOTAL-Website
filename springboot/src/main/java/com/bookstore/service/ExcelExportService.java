package com.bookstore.service;

import com.bookstore.model.Order;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    public ByteArrayInputStream exportOrdersToExcel(List<Order> orders) throws IOException {
        // Export columns per acceptance criteria
        String[] columns = {"Mã đơn", "Khách hàng", "Ngày đặt", "Tổng tiền"};

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh Sách Đơn Hàng");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            // Date + money styles for Excel compatibility
            CreationHelper creationHelper = workbook.getCreationHelper();

            CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));

            CellStyle moneyCellStyle = workbook.createCellStyle();
            moneyCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("#,##0"));

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;

            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(order.getId() != null ? order.getId() : 0);
                
                String customerName = order.getShippingName();
                if (customerName == null || customerName.isEmpty()) {
                    customerName = order.getUser() != null ? order.getUser().getFullName() : "Khách Vãng Lai";
                }
                row.createCell(1).setCellValue(customerName);

                Cell dateCell = row.createCell(2);
                if (order.getOrderDate() != null) {
                    dateCell.setCellValue(java.sql.Timestamp.valueOf(order.getOrderDate()));
                    dateCell.setCellStyle(dateCellStyle);
                } else {
                    dateCell.setCellValue("");
                }

                Cell moneyCell = row.createCell(3);
                double total = order.getFinalTotal() != null ? order.getFinalTotal() : 0.0;
                moneyCell.setCellValue(total);
                moneyCell.setCellStyle(moneyCellStyle);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

}
