/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.util;

import com.huawei.cloududn.dialingtest.model.OperationLog;
import com.huawei.cloududn.dialingtestapp.model.ValidationResult;
import com.huawei.cloududn.dialingtest.model.CaseValidationResult;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Excel文件生成工具类
 * 用于生成操作记录的Excel导出文件
 *
 * @author g00940940
 * @since 2025-01-15
 */
public class ExcelUtil {
    
    /**
     * 生成操作记录Excel文件
     *
     * @param logs 操作记录列表
     * @return Excel文件资源
     */
    public static Resource generateOperationLogsExcel(List<OperationLog> logs) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("操作记录");
            
            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = getHeaders();
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            int rowNum = 1;
            if (logs != null) {
                for (OperationLog log : logs) {
                    Row row = sheet.createRow(rowNum++);
                    
                    row.createCell(0).setCellValue(log.getId() != null ? log.getId() : 0);
                    row.createCell(1).setCellValue(log.getUsername() != null ? log.getUsername() : "");
                    row.createCell(2).setCellValue(log.getOperationType() != null ? log.getOperationType() : "");
                    row.createCell(3).setCellValue(log.getOperationTarget() != null ? log.getOperationTarget() : "");
                    
                    // 设置中文描述
                    row.createCell(4).setCellValue(log.getOperationDescriptionZh() != null ? log.getOperationDescriptionZh() : "");
                    // 设置英文描述
                    row.createCell(5).setCellValue(log.getOperationDescriptionEn() != null ? log.getOperationDescriptionEn() : "");
                    row.createCell(6).setCellValue(log.getOperationTime() != null ? log.getOperationTime() : "");
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 生成文件流
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                byte[] excelBytes = outputStream.toByteArray();
                return new ByteArrayResource(excelBytes);
            }
            
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate Excel file", e);
        }
    }
    
    /**
     * 获取表头
     *
     * @return 表头数组
     */
    private static String[] getHeaders() {
        return new String[]{"ID", "用户名", "操作类型", "操作目标", 
                              "中文描述", "英文描述", "操作时间"};
    }
    
    /**
     * 生成校验结果Excel文件
     *
     * @param validationResult 校验结果
     * @return Excel文件资源
     */
    public static Resource generateValidationResultExcel(ValidationResult validationResult) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("校验结果");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle normalStyle = createNormalCellStyle(workbook);
            
            String[] headers = createValidationResultHeader(sheet, headerStyle);
            fillValidationResultData(sheet, validationResult, normalStyle);
            setValidationResultColumnWidths(sheet, headers);
            
            return generateExcelResource(workbook);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate validation result Excel file", e);
        }
    }
    
    /**
     * 创建普通单元格样式
     *
     * @param workbook 工作簿
     * @return 单元格样式
     */
    private static CellStyle createNormalCellStyle(Workbook workbook) {
        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setBorderTop(BorderStyle.THIN);
        normalStyle.setBorderBottom(BorderStyle.THIN);
        normalStyle.setBorderLeft(BorderStyle.THIN);
        normalStyle.setBorderRight(BorderStyle.THIN);
        normalStyle.setWrapText(true);
        normalStyle.setVerticalAlignment(VerticalAlignment.TOP);
        return normalStyle;
    }
    
    /**
     * 创建校验结果表头
     *
     * @param sheet 工作表
     * @param headerStyle 表头样式
     * @return 表头数组
     */
    private static String[] createValidationResultHeader(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"用例编号", "用例名称", "脚本校验", "规则校验", "软件包校验", "整体校验", "校验不通过原因"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        return headers;
    }
    
    /**
     * 填充校验结果数据
     *
     * @param sheet 工作表
     * @param validationResult 校验结果
     * @param normalStyle 普通单元格样式
     */
    private static void fillValidationResultData(Sheet sheet, ValidationResult validationResult, CellStyle normalStyle) {
        int rowNum = 1;
        if (validationResult != null && validationResult.getCaseResults() != null) {
            for (CaseValidationResult caseResult : validationResult.getCaseResults()) {
                Row row = sheet.createRow(rowNum++);
                fillValidationResultRow(row, caseResult, normalStyle);
            }
        }
    }
    
    /**
     * 填充单个校验结果数据行
     *
     * @param row 行对象
     * @param caseResult 用例校验结果
     * @param normalStyle 普通单元格样式
     */
    private static void fillValidationResultRow(Row row, CaseValidationResult caseResult, CellStyle normalStyle) {
        row.createCell(0).setCellValue(caseResult.getCaseNumber() != null ? caseResult.getCaseNumber() : "");
        row.createCell(1).setCellValue(caseResult.getCaseName() != null ? caseResult.getCaseName() : "");
        row.createCell(2).setCellValue(caseResult.isScriptMatchValid() ? "通过" : "不通过");
        row.createCell(3).setCellValue(caseResult.isPreprocessRuleValid() ? "通过" : "不通过");
        row.createCell(4).setCellValue(caseResult.isSoftwarePackageValid() ? "通过" : "不通过");
        row.createCell(5).setCellValue(caseResult.isOverallValid() ? "通过" : "不通过");
        
        String invalidReasons = buildInvalidReasons(caseResult.getInvalidReasons());
        Cell reasonCell = row.createCell(6);
        reasonCell.setCellValue(invalidReasons);
        reasonCell.setCellStyle(normalStyle);
        
        for (int i = 0; i < 6; i++) {
            row.getCell(i).setCellStyle(normalStyle);
        }
    }
    
    /**
     * 构建校验不通过原因字符串
     *
     * @param invalidReasons 不通过原因列表
     * @return 原因字符串
     */
    private static String buildInvalidReasons(List<String> invalidReasons) {
        if (invalidReasons == null || invalidReasons.isEmpty()) {
            return "-";
        }
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 0; i < invalidReasons.size(); i++) {
            if (i > 0) {
                reasonBuilder.append("\n");
            }
            reasonBuilder.append(invalidReasons.get(i));
        }
        return reasonBuilder.toString();
    }
    
    /**
     * 设置校验结果列宽
     *
     * @param sheet 工作表
     * @param headers 表头数组
     */
    private static void setValidationResultColumnWidths(Sheet sheet, String[] headers) {
        for (int i = 0; i < headers.length; i++) {
            if (i == 6) {
                sheet.setColumnWidth(i, 8000);
            } else {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 2000) {
                    sheet.setColumnWidth(i, 2000);
                }
            }
        }
    }
    
    /**
     * 生成Excel文件资源
     *
     * @param workbook 工作簿
     * @return Excel文件资源
     * @throws IOException IO异常
     */
    private static Resource generateExcelResource(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();
            return new ByteArrayResource(excelBytes);
        }
    }
    
    /**
     * 创建表头样式
     *
     * @param workbook 工作簿
     * @return 表头样式
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // 设置背景色
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // 设置字体
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        
        // 设置对齐方式
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }
}
