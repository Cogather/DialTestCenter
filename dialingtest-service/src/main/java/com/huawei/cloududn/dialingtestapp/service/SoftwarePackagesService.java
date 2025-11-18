package com.huawei.cloududn.dialingtestapp.service;

import com.huawei.cloududn.dialingtestapp.dao.SoftwarePackageDao;
import com.huawei.cloududn.dialingtestapp.dao.TestCaseDao;
import com.huawei.cloududn.dialingtestapp.entity.SoftwarePackage;
import com.huawei.cloududn.dialingtest.model.SoftwarePackageInfo;
import com.huawei.cloududn.dialingtest.model.SoftwarePackageListResponseData;
import com.huawei.cloududn.dialingtestapp.util.OperationLogUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 软件包管理服务
 * 
 * @author Generated
 */
@Service
@Transactional
public class SoftwarePackagesService {
    
    private static final Logger logger = LoggerFactory.getLogger(SoftwarePackagesService.class);
    
    @Autowired
    private SoftwarePackageDao softwarePackageDao;
    
    @Autowired
    private TestCaseDao testCaseDao;
    
    @Autowired
    private OperationLogUtil operationLogUtil;
    
    /**
     * 分页获取软件包列表
     */
    public SoftwarePackageListResponseData getSoftwarePackageList(Integer page, Integer pageSize, String keyword) {
        // 计算偏移量
        int offset = (page - 1) * pageSize;
        
        // 查询数据
        List<SoftwarePackageInfo> packages = softwarePackageDao.getSoftwarePackageList(keyword, offset, pageSize);
        int total = softwarePackageDao.countSoftwarePackageCount(keyword);
        
        // 构建响应数据
        SoftwarePackageListResponseData data = new SoftwarePackageListResponseData();
        data.setPage(page);
        data.setPageSize(pageSize);
        data.setTotal(total);
        data.setData(packages);
        
        return data;
    }
    
    /**
     * 根据ID获取软件包详情
     */
    public SoftwarePackageInfo getSoftwarePackageById(Long id) {
        return softwarePackageDao.getSoftwarePackageById(id);
    }
    
    /**
     * 更新软件包描述
     */
    public SoftwarePackageInfo updateSoftwarePackage(Long id, String description) {
        softwarePackageDao.updateSoftwarePackageDescription(id, description);
        return softwarePackageDao.getSoftwarePackageById(id);
    }
    
    /**
     * 删除软件包
     */
    public boolean deleteSoftwarePackage(Long id) {
        // 检查是否存在
        SoftwarePackageInfo packageInfo = softwarePackageDao.getSoftwarePackageById(id);
        if (packageInfo == null) {
            return false;
        }
        
        // 删除记录
        int deleted = softwarePackageDao.deleteSoftwarePackage(id);
        return deleted > 0;
    }
    
    /**
     * 检查软件包是否被测试用例集引用
     */
    public boolean isReferencedByTestCaseSet(Long packageId) {
        SoftwarePackageInfo packageInfo = softwarePackageDao.getSoftwarePackageById(packageId);
        if (packageInfo == null) {
            return false;
        }
        
        // 查找是否有测试用例引用了这个软件包
        return softwarePackageDao.isSoftwarePackageReferenced(packageInfo.getSoftwareName()) > 0;
    }
    
    /**
     * 下载软件包文件
     */
    public Resource downloadSoftwarePackages(List<Long> packageIds) {
        if (packageIds.size() == 1) {
            // 单个文件下载
            Long packageId = packageIds.get(0);
            SoftwarePackageInfo packageInfo = softwarePackageDao.getSoftwarePackageById(packageId);
            if (packageInfo != null) {
                byte[] fileContent = softwarePackageDao.getSoftwarePackageFileContent(packageId);
                return new ByteArrayResource(fileContent);
            }
            return null;
        } else {
            // 批量下载，打包为ZIP
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                
                for (Long packageId : packageIds) {
                    SoftwarePackageInfo packageInfo = softwarePackageDao.getSoftwarePackageById(packageId);
                    if (packageInfo != null) {
                        byte[] fileContent = softwarePackageDao.getSoftwarePackageFileContent(packageId);
                        if (fileContent != null && fileContent.length > 0) {
                            ZipEntry entry = new ZipEntry(packageInfo.getSoftwareName());
                            zos.putNextEntry(entry);
                            zos.write(fileContent);
                            zos.closeEntry();
                        }
                    }
                }
                
                zos.finish();
                zos.flush();
            } catch (IOException e) {
                logger.error("Failed to create ZIP file for batch download", e);
                throw new IllegalStateException("创建ZIP文件失败", e);
            }
            
            byte[] zipContent = baos.toByteArray();
            if (zipContent.length == 0) {
                logger.warn("Generated ZIP file is empty for package IDs: {}", packageIds);
                throw new IllegalArgumentException("无法生成ZIP文件，没有可下载的软件包");
            }
            
            return new ByteArrayResource(zipContent);
        }
    }
    
    /**
     * 根据ID获取软件包名称
     */
    public String getSoftwarePackageNameById(Long id) {
        SoftwarePackageInfo packageInfo = softwarePackageDao.getSoftwarePackageById(id);
        return packageInfo != null ? packageInfo.getSoftwareName() : null;
    }
    
    /**
     * 上传单个软件包
     */
    public SoftwarePackage uploadSinglePackage(
            MultipartFile file, String description, boolean overwrite, String operatorUsername) 
            throws IOException {
        // 验证文件格式
        validateFileFormat(file.getOriginalFilename());
        
        // 验证文件大小
        validateFileSize(file.getSize());
        
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        // 获取文件内容
        byte[] fileContent = file.getBytes();
        
        // 计算SHA256
        String sha256 = calculateSHA256(fileContent);
        
        // 检查软件名称是否已存在
        String softwareName = file.getOriginalFilename();
        SoftwarePackageInfo existingPackage = null;
        if (softwarePackageDao.checkSoftwareNameExists(softwareName) > 0) {
            if (!overwrite) {
                throw new IllegalArgumentException("软件名称已存在: " + softwareName);
            } else {
                // 获取已存在的软件包信息用于日志记录
                existingPackage = softwarePackageDao.getSoftwarePackageByName(softwareName);
                // 删除已存在的软件包
                deleteExistingPackageByName(softwareName);
            }
        }
        
        // 创建软件包实体
        SoftwarePackage softwarePackage = new SoftwarePackage(
            softwareName,
            description,
            fileContent,
            sha256,
            file.getSize()
        );
        
        // 插入数据库
        softwarePackageDao.insert(softwarePackage);
        
        // 记录操作日志
        if (existingPackage != null) {
            // 覆盖操作
            operationLogUtil.logSoftwarePackageOverwrite(operatorUsername, existingPackage, softwarePackage);
        } else {
            // 新增操作
            operationLogUtil.logSoftwarePackageCreate(operatorUsername, softwarePackage);
        }
        
        return softwarePackage;
    }
    
    /**
     * 上传ZIP包（解压后按单个软件包存储）
     */
    public List<SoftwarePackage> uploadZipPackage(
            MultipartFile file, boolean overwrite, String description, String operatorUsername) 
            throws IOException {
        logger.info("Starting ZIP package upload: {}, overwrite: {}", file.getOriginalFilename(), overwrite);
        
        List<SoftwarePackage> uploadedPackages = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    SoftwarePackage packageResult = processZipEntry(zis, entry, overwrite, description, operatorUsername);
                    if (packageResult != null) {
                        uploadedPackages.add(packageResult);
                    }
                }
            }
        }
        
        validateUploadResult(uploadedPackages);
        logger.info("ZIP package upload completed: {} packages uploaded", uploadedPackages.size());
        
        return uploadedPackages;
    }
    
    /**
     * 处理ZIP包中的单个条目
     */
    private SoftwarePackage processZipEntry(ZipInputStream zis, ZipEntry entry, boolean overwrite, 
                                            String description, String operatorUsername) throws IOException {
        String fileName = entry.getName();
        
        // 跳过系统文件
        if (shouldSkipFile(fileName)) {
            logger.debug("Skipping system file: {}", fileName);
            return null;
                    }
                    
        // 提取文件名
        String simpleFileName = extractFileName(fileName);
                    if (simpleFileName.isEmpty()) {
            logger.debug("Skipping empty filename: {}", fileName);
            return null;
        }
                    
                    // 验证文件格式
        if (!isValidFileFormat(simpleFileName)) {
            logger.debug("Skipping invalid file format: {}", simpleFileName);
            return null;
        }
        
        // 读取并处理文件
        return processPackageFile(zis, simpleFileName, overwrite, description, operatorUsername);
    }
    
    /**
     * 判断是否应该跳过该文件
     */
    private boolean shouldSkipFile(String fileName) {
        return fileName.contains("__MACOSX") || fileName.contains(".DS_Store");
    }
    
    /**
     * 从完整路径中提取文件名
     */
    private String extractFileName(String filePath) {
        int lastIndex = filePath.lastIndexOf("/");
        if (lastIndex == -1) {
            lastIndex = filePath.lastIndexOf("\\");
        }
        return lastIndex >= 0 ? filePath.substring(lastIndex + 1) : filePath;
    }
    
    /**
     * 验证文件格式是否有效
     */
    private boolean isValidFileFormat(String fileName) {
        try {
            validateFileFormat(fileName);
            return true;
                    } catch (IllegalArgumentException e) {
            logger.debug("Invalid file format: {} - {}", fileName, e.getMessage());
            return false;
        }
                    }
                    
    /**
     * 处理软件包文件（读取、验证、保存）
     */
    private SoftwarePackage processPackageFile(ZipInputStream zis, String fileName, boolean overwrite,
                                               String description, String operatorUsername) throws IOException {
                    // 读取文件内容
        byte[] fileContent = readZipEntryContent(zis);
                    
                    // 验证文件大小
                    validateFileSize(fileContent.length);
                    
                    // 计算SHA256
                    String sha256 = calculateSHA256(fileContent);
                    
        // 处理已存在的软件包
        SoftwarePackageInfo existingPackage = handleExistingPackage(fileName, overwrite);
        
        // 创建并保存软件包
        SoftwarePackage softwarePackage = createAndSavePackage(fileName, description, fileContent, sha256);
        
        // 记录操作日志
        logPackageOperation(operatorUsername, existingPackage, softwarePackage);
        
        return softwarePackage;
    }
    
    /**
     * 读取ZIP条目内容
     */
    private byte[] readZipEntryContent(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zis.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        byte[] content = baos.toByteArray();
        baos.close();
        return content;
    }
    
    /**
     * 处理已存在的软件包
     */
    private SoftwarePackageInfo handleExistingPackage(String fileName, boolean overwrite) {
        if (softwarePackageDao.checkSoftwareNameExists(fileName) == 0) {
            return null;
        }
        
                        if (!overwrite) {
            throw new IllegalArgumentException("ZIP包中包含已存在的软件名称: " + fileName);
        }
        
        SoftwarePackageInfo existingPackage = softwarePackageDao.getSoftwarePackageByName(fileName);
        deleteExistingPackageByName(fileName);
        logger.info("Deleted existing package for overwrite: {}", fileName);
        
        return existingPackage;
                    }
                    
    /**
     * 创建并保存软件包
     */
    private SoftwarePackage createAndSavePackage(String fileName, String description, 
                                                  byte[] fileContent, String sha256) {
                    SoftwarePackage softwarePackage = new SoftwarePackage(
            fileName,
            description,
                        fileContent,
                        sha256,
                        (long) fileContent.length
                    );
                    
                    softwarePackageDao.insert(softwarePackage);
        logger.debug("Saved software package: {}", fileName);
        
        return softwarePackage;
    }
    
    /**
     * 记录软件包操作日志
     */
    private void logPackageOperation(String operatorUsername, SoftwarePackageInfo existingPackage, 
                                     SoftwarePackage softwarePackage) {
                    if (existingPackage != null) {
                        operationLogUtil.logSoftwarePackageOverwrite(operatorUsername, existingPackage, softwarePackage);
            logger.info("Logged overwrite operation for package: {}", softwarePackage.getSoftwareName());
                    } else {
                        operationLogUtil.logSoftwarePackageCreate(operatorUsername, softwarePackage);
            logger.info("Logged create operation for package: {}", softwarePackage.getSoftwareName());
        }
    }
    
    /**
     * 验证上传结果
     */
    private void validateUploadResult(List<SoftwarePackage> uploadedPackages) {
        if (uploadedPackages.isEmpty()) {
            throw new IllegalArgumentException("ZIP包中没有找到有效的软件包文件（.apk或.ipa）");
        }
    }
    
    /**
     * 获取单个软件包的文件内容
     */
    public byte[] getSoftwarePackageFileContent(Long id) {
        return softwarePackageDao.getSoftwarePackageFileContent(id);
    }
    
    /**
     * 验证文件格式
     */
    private void validateFileFormat(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        String lowerCaseFileName = fileName.toLowerCase();
        if (!lowerCaseFileName.endsWith(".apk") && !lowerCaseFileName.endsWith(".ipa")) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持.apk和.ipa文件");
        }
    }
    
    /**
     * 验证文件大小
     */
    private void validateFileSize(long fileSize) {
        final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
        
        if (fileSize <= 0) {
            throw new IllegalArgumentException("文件大小不能为0");
        }
        
        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制（100MB）");
        }
    }
    
    /**
     * 计算SHA256哈希值
     */
    private String calculateSHA256(byte[] data) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA256算法不可用", e);
        }
    }
    
    /**
     * 根据软件名称删除已存在的软件包
     */
    private void deleteExistingPackageByName(String softwareName) {
        softwarePackageDao.deleteSoftwarePackageByName(softwareName);
    }
}
