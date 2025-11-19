package com.huawei.cloududn.dialingtestapp.service.basicDataManage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.PreprocessRulePackageDao;
import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.PreprocessRuleDao;
import com.huawei.cloududn.dialingtestapp.entity.basicDataManage.PreprocessRulePackageEntity;
import com.huawei.cloududn.dialingtest.model.*;
import com.huawei.cloududn.dialingtestapp.util.OperationLogUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 预处理规则管理服务
 * 
 * @author g00940940
 * @since 2025-01-27
 */
@Service
@Transactional
public class PreprocessRuleService {
    private static final Logger logger = LoggerFactory.getLogger(PreprocessRuleService.class);
    
    @Autowired
    private PreprocessRulePackageDao preprocessRulePackageDao;
    
    @Autowired
    private PreprocessRuleDao preprocessRuleDao;
    
    @Autowired
    private OperationLogUtil operationLogUtil;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取ZIP包列表
     */
    public PreprocessRulePackageListResponse getPreprocessRulePackages(
            Integer page, Integer pageSize, String businessZh, String keyword) {
        
        PreprocessRulePackageListResponse response = new PreprocessRulePackageListResponse();
        
        try {
            // 计算偏移量
            int offset = (page - 1) * pageSize;
            
            // 查询总数
            int total = preprocessRulePackageDao.countByConditions(businessZh, keyword);
            
        // 查询数据
        List<PreprocessRulePackageEntity> packages = preprocessRulePackageDao.findByConditions(
            businessZh, keyword, offset, pageSize);
            
            // 构建响应数据
            PreprocessRulePackageListResponseData data = new PreprocessRulePackageListResponseData();
            data.setPage(page);
            data.setPageSize(pageSize);
            data.setTotal(total);
            
            // 转换实体类为模型类
            List<PreprocessRulePackage> packageModels = packages.stream()
                .map(this::convertToModel)
                .collect(java.util.stream.Collectors.toList());
            data.setData(packageModels);
            
            response.setSuccess(true);
            response.setMessage("获取ZIP包列表成功");
            response.setData(data);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("获取ZIP包列表失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 删除ZIP包
     */
    public void deletePreprocessRulePackage(Long id, String operatorUsername) {
        // 查询ZIP包信息
        PreprocessRulePackageEntity rulePackage = preprocessRulePackageDao.findById(id);
        if (rulePackage == null) {
            throw new IllegalArgumentException("ZIP包不存在");
        }
        
        // 删除ZIP包（级联删除会自动删除关联的规则）
        preprocessRulePackageDao.deleteById(id);
        
        // 记录操作日志
        operationLogUtil.logPreprocessRulePackageDelete(operatorUsername, rulePackage.getPackageName());
    }
    
    /**
     * 下载ZIP包
     */
    public byte[] downloadPreprocessRulePackage(Long id) {
        PreprocessRulePackageEntity rulePackage = preprocessRulePackageDao.findById(id);
        if (rulePackage == null) {
            throw new IllegalArgumentException("ZIP包不存在");
        }
        
        return rulePackage.getFileContent();
    }
    
    /**
     * 获取预处理规则列表
     */
    public PreprocessRuleListResponse getPreprocessRules(
            Integer page, Integer pageSize, String businessZh, String category, 
            String appName, String ruleName, String keyword) {
        
        PreprocessRuleListResponse response = new PreprocessRuleListResponse();
        
        try {
            // 计算偏移量
            int offset = (page - 1) * pageSize;
            
            // 查询总数
            int total = preprocessRuleDao.countByConditions(businessZh, category, appName, ruleName, keyword);
            
            // 查询数据
            List<PreprocessRule> rules = preprocessRuleDao.findByConditions(
                businessZh, category, appName, ruleName, keyword, offset, pageSize);
            
            // 构建响应数据
            PreprocessRuleListResponseData data = new PreprocessRuleListResponseData();
            data.setPage(page);
            data.setPageSize(pageSize);
            data.setTotal(total);
            data.setData(rules);
            
            response.setSuccess(true);
            response.setMessage("获取预处理规则列表成功");
            response.setData(data);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("获取预处理规则列表失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 获取所有业务类型
     */
    public BusinessTypeListResponse getAllBusinessTypes() {
        BusinessTypeListResponse response = new BusinessTypeListResponse();
        
        try {
            List<String> businessTypes = preprocessRuleDao.findDistinctBusinessTypes();
            
            response.setSuccess(true);
            response.setMessage("获取业务类型列表成功");
            response.setData(businessTypes);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("获取业务类型列表失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 根据业务获取分类
     */
    public CategoryListResponse getCategoriesByBusiness(String businessZh) {
        CategoryListResponse response = new CategoryListResponse();
        
        try {
            List<String> categories = preprocessRuleDao.findDistinctCategoriesByBusiness(businessZh);
            
            response.setSuccess(true);
            response.setMessage("获取分类列表成功");
            response.setData(categories);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("获取分类列表失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 根据业务和分类获取应用名称
     */
    public AppNameListResponse getAppNamesByBusinessAndCategory(String businessZh, String category) {
        AppNameListResponse response = new AppNameListResponse();
        
        try {
            List<String> appNames = preprocessRuleDao.findDistinctAppNamesByBusinessAndCategory(businessZh, category);
            
            response.setSuccess(true);
            response.setMessage("获取应用名称列表成功");
            response.setData(appNames);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("获取应用名称列表失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 根据业务、分类和应用获取规则名称
     */
    public RuleNameListResponse getRuleNamesByBusinessAndCategoryAndApp(
            String businessZh, String category, String appName) {
        RuleNameListResponse response = new RuleNameListResponse();
        
        try {
            List<String> ruleNames = preprocessRuleDao.findDistinctRuleNamesByBusinessAndCategoryAndApp(
                businessZh, category, appName);
            
            response.setSuccess(true);
            response.setMessage("获取规则名称列表成功");
            response.setData(ruleNames);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("获取规则名称列表失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 获取筛选选项
     */
    public FilterOptionsResponse getFilterOptions() {
        FilterOptionsResponse response = new FilterOptionsResponse();
        
        try {
            PreprocessRuleFilterOptions options = new PreprocessRuleFilterOptions();
            
            // 获取所有业务类型
            options.setBusinessTypes(preprocessRuleDao.findDistinctBusinessTypes());
            
            // 获取所有分类
            options.setCategories(preprocessRuleDao.findDistinctCategories());
            
            // 获取所有应用名称
            options.setAppNames(preprocessRuleDao.findDistinctAppNames());
            
            // 获取所有规则名称
            options.setRuleNames(preprocessRuleDao.findDistinctRuleNames());
            
            response.setSuccess(true);
            response.setMessage("获取筛选选项成功");
            response.setData(options);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("获取筛选选项失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 上传预处理规则ZIP包
     */
    public String uploadPreprocessRulePackage(MultipartFile file, String businessZh, String businessEn, 
                                            String description, String operatorUsername, boolean forceOverwrite) throws IOException {
        
        // 1. 文件验证
        validateZipFile(file);
        
        String packageName = file.getOriginalFilename();
        
        // 2. 检查包名和业务类型是否已存在
        PreprocessRulePackageEntity existingPackage = 
                preprocessRulePackageDao.findByPackageNameAndBusiness(packageName, businessZh);
        
        if (existingPackage != null && !forceOverwrite) {
            // 已存在同名包且未强制覆盖，抛出异常
            throw new IllegalArgumentException("该业务类型下已存在同名ZIP包: " + packageName);
        }
        
        // 3. 读取文件内容（只读取一次，避免 MultipartFile 被消费后无法再次读取）
        byte[] fileContent = file.getBytes();
        logger.info("Read file content, size: {} bytes", fileContent.length);
        
        PreprocessRulePackageEntity preprocessRulePackage;
        
        if (existingPackage != null && forceOverwrite) {
            // 覆盖模式：更新现有记录
            logger.info("Overwriting existing package: {} (ID: {})", packageName, existingPackage.getId());
            
            existingPackage.setBusinessZh(businessZh);
            existingPackage.setBusinessEn(businessEn);
            existingPackage.setFileContent(fileContent);
            existingPackage.setFileSize(file.getSize());
            existingPackage.setDescription(description);
            
            preprocessRulePackageDao.update(existingPackage);
            preprocessRulePackage = existingPackage;
            
            // 删除该包关联的所有旧规则（ON DELETE CASCADE会自动删除，但我们手动删除以便重新插入）
            preprocessRuleDao.deleteByPackageId(existingPackage.getId());
            logger.info("Deleted existing rules for package ID: {}", existingPackage.getId());
            
        } else {
            // 新增模式：创建新记录
            preprocessRulePackage = new PreprocessRulePackageEntity();
            preprocessRulePackage.setPackageName(packageName);
            preprocessRulePackage.setBusinessZh(businessZh);
            preprocessRulePackage.setBusinessEn(businessEn);
            preprocessRulePackage.setFileContent(fileContent);
            preprocessRulePackage.setFileSize(file.getSize());
            preprocessRulePackage.setDescription(description);
            
            preprocessRulePackageDao.insert(preprocessRulePackage);
        }
        
        // 4. 解析ZIP文件并提取规则（使用已读取的文件内容）
        int rulesCount = parseAndSaveRulesFromZip(fileContent, businessZh, businessEn, 
                                                   preprocessRulePackage.getId(), forceOverwrite);
        
        // 5. 记录操作日志
        operationLogUtil.logPreprocessRulePackageUpload(operatorUsername, packageName, businessZh, businessEn);
        
        return "{\"success\":true,\"message\":\"预处理规则包上传成功，解析出" + rulesCount + 
               "条规则\",\"data\":{\"id\":" + preprocessRulePackage.getId() + 
               ",\"packageName\":\"" + packageName + "\",\"fileSize\":" + file.getSize() + 
               ",\"rulesCount\":" + rulesCount + "}}";
    }
    
    /**
     * 验证ZIP文件
     */
    private void validateZipFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("仅支持ZIP格式文件");
        }
        
        // 检查文件大小（限制为100MB）
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超过100MB");
        }
    }
    
    /**
     * 转换实体类为模型类
     */
    private PreprocessRulePackage convertToModel(PreprocessRulePackageEntity entity) {
        PreprocessRulePackage model = new PreprocessRulePackage();
        model.setId(entity.getId());
        model.setPackageName(entity.getPackageName());
        model.setBusinessZh(entity.getBusinessZh());
        model.setBusinessEn(entity.getBusinessEn());
        model.setFileSize(entity.getFileSize());
        model.setDescription(entity.getDescription());
        return model;
    }
    
    /**
     * 从ZIP文件中解析并保存预处理规则
     * 
     * @param zipFileContent ZIP文件内容
     * @param businessZh 业务类型（中文）
     * @param businessEn 业务类型（英文）
     * @param packageId ZIP包ID
     * @param forceOverwrite 是否强制覆盖同名规则
     * @return 解析出的规则数量
     */
    private int parseAndSaveRulesFromZip(byte[] zipFileContent, String businessZh, 
                                        String businessEn, Long packageId, boolean forceOverwrite) throws IOException {
        int rulesCount = 0;
        
        logger.info("Starting to parse ZIP file, size: {} bytes", zipFileContent.length);
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipFileContent);
             ZipInputStream zis = new ZipInputStream(bais)) {
            
            ZipEntry entry;
            int entryCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                logger.debug("Processing ZIP entry {}: {}, isDirectory: {}", entryCount, entry.getName(), entry.isDirectory());
                
                // 只处理JSON文件
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".json")) {
                    logger.info("Found JSON file in ZIP: {}", entry.getName());
                    // 读取JSON文件内容
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    
                    // 解析JSON文件
                    String jsonContent = baos.toString("UTF-8");
                    List<PreprocessRule> rules = parseJsonFileContent(
                        jsonContent, businessZh, businessEn, packageId);
                    
                    // 保存规则到数据库
                    for (PreprocessRule rule : rules) {
                        // 如果是覆盖模式，先删除同名规则
                        if (forceOverwrite) {
                            int deleted = preprocessRuleDao.deleteByRuleNameAndBusiness(rule.getRuleName(), businessZh);
                            if (deleted > 0) {
                                logger.info("Deleted existing rule: {} for business: {}", rule.getRuleName(), businessZh);
                            }
                        }
                        preprocessRuleDao.insert(rule);
                        rulesCount++;
                    }
                }
                zis.closeEntry();
            }
            
            logger.info("Finished processing ZIP file, found {} entries, parsed {} rules", entryCount, rulesCount);
            
        } catch (org.springframework.dao.DuplicateKeyException e) {
            logger.warn("Duplicate rule detected during ZIP parsing", e);
            // 从异常消息中提取重复的规则名称
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("uk_rule_name_business")) {
                throw new IllegalArgumentException("ZIP包中包含已存在的预处理规则，请选择是否覆盖");
            } else {
                throw new IOException("解析ZIP文件失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error("Failed to parse ZIP file", e);
            logger.error("ZIP file size: {} bytes, Exception type: {}, Message: {}", 
                        zipFileContent.length, e.getClass().getName(), e.getMessage());
            throw new IOException("解析ZIP文件失败: " + e.getMessage(), e);
        }
        
        return rulesCount;
    }
    
    /**
     * 解析JSON文件内容并提取预处理规则
     * 
     * @param jsonContent JSON文件内容
     * @param businessZh 业务类型（中文）
     * @param businessEn 业务类型（英文）
     * @param packageId ZIP包ID
     * @return 预处理规则列表
     */
    private List<PreprocessRule> parseJsonFileContent(String jsonContent, String businessZh,
                                                     String businessEn, Long packageId) throws IOException {
        List<PreprocessRule> rules = new ArrayList<>();
        
        try {
            // 添加调试日志
            logger.debug("Parsing JSON content, length: {} bytes", jsonContent.length());
            logger.debug("JSON content preview: {}", jsonContent.substring(0, Math.min(200, jsonContent.length())));
            
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            // 提取category字段
            if (!rootNode.has("category")) {
                throw new IllegalArgumentException("JSON文件缺少category字段");
            }
            String category = rootNode.get("category").asText();
            logger.debug("Extracted category: {}", category);
            
            // 解析apps对象（非自定义规则）
            if (rootNode.has("apps")) {
                JsonNode appsNode = rootNode.get("apps");
                if (appsNode.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> appsIterator = appsNode.fields();
                    while (appsIterator.hasNext()) {
                        Map.Entry<String, JsonNode> appEntry = appsIterator.next();
                        String appName = appEntry.getKey();
                        JsonNode appValueNode = appEntry.getValue();
                        
                        // 生成规则名称：category-appName
                        String ruleName = category + "-" + appName;
                        
                        // 创建规则模型
                        PreprocessRule rule = new PreprocessRule();
                        rule.setRuleName(ruleName);
                        rule.setBusinessZh(businessZh);
                        rule.setBusinessEn(businessEn);
                        rule.setCategory(category);
                        rule.setAppName(appName);
                        rule.setContent(appValueNode.toString());
                        rule.setIsCustom(false);
                        rule.setPackageId(packageId);
                        
                        rules.add(rule);
                    }
                }
            }
            
            // 解析custom_rules对象（自定义规则）
            if (rootNode.has("custom_rules")) {
                JsonNode customRulesNode = rootNode.get("custom_rules");
                if (customRulesNode.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> customRulesIterator = customRulesNode.fields();
                    while (customRulesIterator.hasNext()) {
                        Map.Entry<String, JsonNode> customRuleEntry = customRulesIterator.next();
                        String customRuleKey = customRuleEntry.getKey();
                        JsonNode customRuleNode = customRuleEntry.getValue();
                        
                        // 遍历自定义规则中的每个app
                        if (customRuleNode.isObject()) {
                            Iterator<Map.Entry<String, JsonNode>> appIterator = customRuleNode.fields();
                            while (appIterator.hasNext()) {
                                Map.Entry<String, JsonNode> appEntry = appIterator.next();
                                String appName = appEntry.getKey();
                                JsonNode appValueNode = appEntry.getValue();
                                
                                // 生成规则名称：category-customRuleKey-appName
                                String ruleName = category + "-" + customRuleKey + "-" + appName;
                                
                                // 创建规则模型
                                PreprocessRule rule = new PreprocessRule();
                                rule.setRuleName(ruleName);
                                rule.setBusinessZh(businessZh);
                                rule.setBusinessEn(businessEn);
                                rule.setCategory(category);
                                rule.setAppName(appName);
                                rule.setContent(appValueNode.toString());
                                rule.setIsCustom(true);
                                rule.setPackageId(packageId);
                                
                                rules.add(rule);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to parse JSON content", e);
            logger.error("Exception type: {}, Message: {}", e.getClass().getName(), e.getMessage());
            throw new IOException("解析JSON文件失败: " + e.getMessage(), e);
        }
        
        logger.info("Successfully parsed {} rules from JSON", rules.size());
        return rules;
    }
}
