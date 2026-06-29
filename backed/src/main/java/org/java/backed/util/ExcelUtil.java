package org.java.backed.util;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.java.backed.entity.StudentDormitory;
import org.java.backed.entity.PaymentBill;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Excel处理工具类 (基于POI)
 */
@Slf4j
public class ExcelUtil {

    /**
     * 解析学生信息Excel文件
     */
    public static List<StudentDormitory> parseStudents(InputStream is) throws IOException {
        List<StudentDormitory> list = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    StudentDormitory student = new StudentDormitory();
                    student.setStudentName(getCellString(row, 0));
                    student.setStudentNo(getCellString(row, 1));
                    student.setDormitoryNo(getCellString(row, 2));
                    student.setPhone(getCellString(row, 3));
                    String dateStr = getCellString(row, 4);
                    student.setCheckInDate(dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now());
                    student.setPaymentStatus("UNPAID");
                    list.add(student);
                } catch (Exception e) {
                    log.warn("解析学生数据行失败: 行号={}", i + 1, e);
                }
            }
        }
        return list;
    }

    /**
     * 生成学生信息Excel
     */
    public static byte[] exportStudents(List<StudentDormitory> list) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("学生信息");
            Row header = sheet.createRow(0);
            String[] titles = {"姓名", "学号", "宿舍号", "联系电话", "入住时间"};
            for (int i = 0; i < titles.length; i++) {
                header.createCell(i).setCellValue(titles[i]);
            }

            for (int i = 0; i < list.size(); i++) {
                Row row = sheet.createRow(i + 1);
                StudentDormitory s = list.get(i);
                row.createCell(0).setCellValue(s.getStudentName());
                row.createCell(1).setCellValue(s.getStudentNo());
                row.createCell(2).setCellValue(s.getDormitoryNo());
                row.createCell(3).setCellValue(s.getPhone() != null ? s.getPhone() : "");
                row.createCell(4).setCellValue(s.getCheckInDate() != null ? s.getCheckInDate().toString() : "");
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * 生成账单Excel
     */
    public static byte[] exportBills(List<PaymentBill> list) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("缴费账单");
            Row header = sheet.createRow(0);
            String[] titles = {"账单编号", "学生姓名", "学号", "宿舍号", "收费项目", "学期", "应缴金额", "已缴金额", "截止日期", "状态"};
            for (int i = 0; i < titles.length; i++) {
                header.createCell(i).setCellValue(titles[i]);
            }

            for (int i = 0; i < list.size(); i++) {
                Row row = sheet.createRow(i + 1);
                PaymentBill b = list.get(i);
                row.createCell(0).setCellValue(b.getBillNo());
                row.createCell(1).setCellValue(b.getStudentName() != null ? b.getStudentName() : "");
                row.createCell(2).setCellValue(b.getStudentNo() != null ? b.getStudentNo() : "");
                row.createCell(3).setCellValue(b.getDormitoryNo() != null ? b.getDormitoryNo() : "");
                row.createCell(4).setCellValue(b.getFeeItemName() != null ? b.getFeeItemName() : "");
                row.createCell(5).setCellValue(b.getSemester());
                row.createCell(6).setCellValue(b.getAmount() != null ? b.getAmount().doubleValue() : 0);
                row.createCell(7).setCellValue(b.getPaidAmount() != null ? b.getPaidAmount().doubleValue() : 0);
                row.createCell(8).setCellValue(b.getDueDate() != null ? b.getDueDate().toString() : "");
                row.createCell(9).setCellValue(b.getStatus());
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private static String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
}
