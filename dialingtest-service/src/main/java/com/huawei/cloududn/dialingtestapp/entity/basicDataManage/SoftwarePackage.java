/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.entity.basicDataManage;

import java.util.Objects;

/**
 * 软件包实体类
 * 
 * @author g00940940
 * @since 2025-09-29
 */
public class SoftwarePackage {
    
    private Long id;
    private String softwareName;
    private String description;
    private byte[] fileContent;
    private String fileSha256;
    private Long fileSize;
    
    /**
     * 默认构造函数
     */
    public SoftwarePackage() {
        // Default constructor
    }
    
    /**
     * 构造函数
     *
     * @param softwareName 软件名称
     * @param description 描述信息
     * @param fileContent 文件内容
     * @param fileSha256 SHA256哈希值
     * @param fileSize 文件大小
     */
    public SoftwarePackage(String softwareName, String description, byte[] fileContent, 
                          String fileSha256, Long fileSize) {
        this.softwareName = softwareName;
        this.description = description;
        this.fileContent = fileContent;
        this.fileSha256 = fileSha256;
        this.fileSize = fileSize;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSoftwareName() {
        return softwareName;
    }
    
    public void setSoftwareName(String softwareName) {
        this.softwareName = softwareName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public byte[] getFileContent() {
        return fileContent;
    }
    
    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }
    
    public String getFileSha256() {
        return fileSha256;
    }
    
    public void setFileSha256(String fileSha256) {
        this.fileSha256 = fileSha256;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SoftwarePackage that = (SoftwarePackage) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(softwareName, that.softwareName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, softwareName);
    }
    
    @Override
    public String toString() {
        return "SoftwarePackage{" +
                "id=" + id +
                ", softwareName='" + softwareName + '\'' +
                ", description='" + description + '\'' +
                ", fileSha256='" + fileSha256 + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}

