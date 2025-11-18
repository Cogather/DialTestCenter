package com.huawei.cloududn.dialingtestapp.service;

import com.huawei.cloududn.dialingtest.model.TestCaseInfo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel文件解析服务
 * 
 * @author Generated
 */
@Service
public class ExcelParseService {
    
    /**
     * 解析Excel文件
     */
    public List<TestCaseInfo> parseExcel(byte[] excelData) {
        List<TestCaseInfo> testCases = new ArrayList<>();
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(excelData);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel文件格式错误：缺少表头行");
            }
            
            Map<String, Integer> columnIndexMap = buildColumnIndexMap(headerRow);
            
            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                TestCaseInfo testCase = new TestCaseInfo();
                testCase.setCaseName(getCellValue(row, columnIndexMap.get("用例_名称")));
                testCase.setCaseNumber(getCellValue(row, columnIndexMap.get("用例_编号")));
                testCase.setTestSteps(getCellValue(row, columnIndexMap.get("用例_测试步骤")));
                testCase.setExpectedResult(getCellValue(row, columnIndexMap.get("用例_预期结果")));
                testCase.setBusinessCategory(getCellValue(row, columnIndexMap.get("用例_业务大类")));
                testCase.setAppName(getCellValue(row, columnIndexMap.get("用例_App")));
                testCase.setDependenciesPackage(getCellValue(row, columnIndexMap.get("依赖软件包")));
                testCase.setDependenciesRule(getCellValue(row, columnIndexMap.get("依赖规则")));
                testCase.setEnvironmentConfig(parseEnvironmentConfig(getCellValue(row, columnIndexMap.get("环境配置"))));
                
                testCases.add(testCase);
            }
        } catch (IOException e) {
            throw new IllegalStateException("解析Excel文件失败", e);
        }
        
        return testCases;
    }
    
    /**
     * 构建列索引映射
     */
    private Map<String, Integer> buildColumnIndexMap(Row headerRow) {
        Map<String, Integer> columnIndexMap = new HashMap<>();
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String columnName = getCellValue(cell);
                columnIndexMap.put(columnName, i);
            }
        }
        
        return columnIndexMap;
    }
    
    /**
     * 获取单元格值
     */
    private String getCellValue(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return null;
        }
        
        Cell cell = row.getCell(columnIndex);
        return getCellValue(cell);
    }
    
    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    /**
     * 解析环境配置JSON
     */
    private Object parseEnvironmentConfig(String environmentConfig) {
        if (environmentConfig == null || environmentConfig.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 简单的JSON解析，实际项目中可以使用Jackson等库
            return environmentConfig;
        } catch (Exception e) {
            return environmentConfig;
        }
    }
    
}
