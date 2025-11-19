package com.huawei.cloududn.dialingtestapp.service.basicDataManage;

import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 压缩包解析服务
 * 
 * @author Generated
 */
@Service
public class ArchiveParseService {
    
    /**
     * 解析ZIP压缩包
     */
    public ArchiveParseResult parseArchive(byte[] archiveData) {
        ArchiveParseResult result = new ArchiveParseResult();
        
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(archiveData))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // 查找cases.xlsx文件
                if (entryName.equals("cases.xlsx")) {
                    byte[] excelData = readEntryContent(zipInputStream);
                    result.setExcelData(excelData);
                }
                
                // 查找scripts目录下的Python文件
                if (entryName.startsWith("scripts/") && entryName.endsWith(".py")) {
                    String scriptFileName = entryName.substring(entryName.lastIndexOf("/") + 1);
                    result.addScriptFileName(scriptFileName);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("解析压缩包失败", e);
        }
        
        return result;
    }
    
    /**
     * 读取ZIP条目内容
     */
    private byte[] readEntryContent(ZipInputStream zipInputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = zipInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }
    
}
