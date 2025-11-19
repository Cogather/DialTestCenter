/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.basicDataManage;

import java.util.ArrayList;
import java.util.List;

/**
 * 压缩包解析结果
 * 
 * @author g00940940
 * @since 2025-09-23
 */
public class ArchiveParseResult {
    private byte[] excelData;
    private List<String> scriptFileNames;
    
    public ArchiveParseResult() {
        this.scriptFileNames = new ArrayList<>();
    }
    
    public ArchiveParseResult(byte[] excelData, List<String> scriptFileNames) {
        this.excelData = excelData;
        this.scriptFileNames = scriptFileNames;
    }
    
    public byte[] getExcelData() {
        return excelData;
    }
    
    public void setExcelData(byte[] excelData) {
        this.excelData = excelData;
    }
    
    public List<String> getScriptFileNames() {
        return scriptFileNames;
    }
    
    public void setScriptFileNames(List<String> scriptFileNames) {
        this.scriptFileNames = scriptFileNames;
    }
    
    public void addScriptFileName(String scriptFileName) {
        if (this.scriptFileNames == null) {
            this.scriptFileNames = new ArrayList<>();
        }
        this.scriptFileNames.add(scriptFileName);
    }
}
