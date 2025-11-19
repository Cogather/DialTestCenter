/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.entity.basicDataManage;

/**
 * 预处理规则ZIP包实体类
 * 
 * @author g00940940
 * @since 2025-01-27
 */
public class PreprocessRulePackageEntity {
    
    private Long id;
    private String packageName;
    private String businessZh;
    private String businessEn;
    private byte[] fileContent;
    private Long fileSize;
    private String description;
    
    // 默认构造函数
    public PreprocessRulePackageEntity() {
    }
    
    // 带参数构造函数
    public PreprocessRulePackageEntity(String packageName, String businessZh, String businessEn, 
                                     byte[] fileContent, Long fileSize, String description) {
        this.packageName = packageName;
        this.businessZh = businessZh;
        this.businessEn = businessEn;
        this.fileContent = fileContent;
        this.fileSize = fileSize;
        this.description = description;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    public String getBusinessZh() {
        return businessZh;
    }
    
    public void setBusinessZh(String businessZh) {
        this.businessZh = businessZh;
    }
    
    public String getBusinessEn() {
        return businessEn;
    }
    
    public void setBusinessEn(String businessEn) {
        this.businessEn = businessEn;
    }
    
    public byte[] getFileContent() {
        return fileContent;
    }
    
    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PreprocessRulePackageEntity that = (PreprocessRulePackageEntity) o;
        return java.util.Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "PreprocessRulePackageEntity{" +
                "id=" + id +
                ", packageName='" + packageName + '\'' +
                ", businessZh='" + businessZh + '\'' +
                ", businessEn='" + businessEn + '\'' +
                ", fileSize=" + fileSize +
                ", description='" + description + '\'' +
                '}';
    }
}
