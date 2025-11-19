/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.entity.basicDataManage;

import java.util.Objects;

/**
 * 应用类型实体类
 * 用于存储和管理应用类型信息，包括业务大类、应用名称和描述
 *
 * @author g00940940
 * @since 2025-01-24
 */
public class AppType {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 业务大类
     */
    private String businessCategory;
    
    /**
     * 应用名称
     */
    private String appName;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 默认构造函数
     */
    public AppType() {
    }
    
    /**
     * 带参数的构造函数
     *
     * @param businessCategory 业务大类
     * @param appName 应用名称
     * @param description 描述信息
     */
    public AppType(String businessCategory, String appName, String description) {
        this.businessCategory = businessCategory;
        this.appName = appName;
        this.description = description;
    }
    
    /**
     * 获取主键ID
     *
     * @return 主键ID
     */
    public Long getId() {
        return id;
    }
    
    /**
     * 设置主键ID
     *
     * @param id 主键ID
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * 获取业务大类
     *
     * @return 业务大类
     */
    public String getBusinessCategory() {
        return businessCategory;
    }
    
    /**
     * 设置业务大类
     *
     * @param businessCategory 业务大类
     */
    public void setBusinessCategory(String businessCategory) {
        this.businessCategory = businessCategory;
    }
    
    /**
     * 获取应用名称
     *
     * @return 应用名称
     */
    public String getAppName() {
        return appName;
    }
    
    /**
     * 设置应用名称
     *
     * @param appName 应用名称
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    /**
     * 获取描述信息
     *
     * @return 描述信息
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 设置描述信息
     *
     * @param description 描述信息
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * 判断对象是否相等
     *
     * @param o 比较对象
     * @return 是否相等
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AppType appType = (AppType) o;
        return Objects.equals(id, appType.id) &&
               Objects.equals(businessCategory, appType.businessCategory) &&
               Objects.equals(appName, appType.appName) &&
               Objects.equals(description, appType.description);
    }
    
    /**
     * 计算哈希值
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, businessCategory, appName, description);
    }
    
    /**
     * 转换为字符串表示
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "AppType{" +
               "id=" + id +
               ", businessCategory='" + businessCategory + '\'' +
               ", appName='" + appName + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}
