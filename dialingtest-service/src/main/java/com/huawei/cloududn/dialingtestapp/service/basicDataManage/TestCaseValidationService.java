package com.huawei.cloududn.dialingtestapp.service.basicDataManage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.PreprocessRuleDao;
import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.SoftwarePackageDao;
import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.TestCaseDao;
import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.TestCaseSetDao;
import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.ValidationResultDao;
import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.ValidationTaskDao;
import com.huawei.cloududn.dialingtestapp.entity.basicDataManage.ValidationTask;
import com.huawei.cloududn.dialingtest.model.CaseValidationResult;
import com.huawei.cloududn.dialingtest.model.PreprocessRule;
import com.huawei.cloududn.dialingtest.model.SoftwarePackageInfo;
import com.huawei.cloududn.dialingtest.model.TestCase;
import com.huawei.cloududn.dialingtest.model.TestCaseSet;
import com.huawei.cloududn.dialingtestapp.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 用例集校验服务
 *
 * @author g00940940
 * @since 2025-10-30
 */
@Service
public class TestCaseValidationService {
    private static final Logger logger = LoggerFactory.getLogger(TestCaseValidationService.class);

    @Autowired
    private TestCaseSetDao testCaseSetDao;

    @Autowired
    private TestCaseDao testCaseDao;

    @Autowired
    private ArchiveParseService archiveParseService;

    @Autowired
    private PreprocessRuleDao preprocessRuleDao;

    @Autowired
    private SoftwarePackageDao softwarePackageDao;
    
    @Autowired
    private ValidationTaskDao validationTaskDao;
    
    @Autowired
    private ValidationResultDao validationResultDao;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行用例集完整校验
     *
     * @param testCaseSetId 用例集ID
     * @return 校验结果
     */
    public ValidationResult validateTestCaseSet(Long testCaseSetId) {
        TestCaseSet set = testCaseSetDao.findById(testCaseSetId);
        if (set == null) {
            throw new IllegalArgumentException("Test case set not found: " + testCaseSetId);
        }

        List<TestCase> testCases = testCaseDao.findAllByTestCaseSetId(testCaseSetId);
        ValidationResult summary = new ValidationResult();
        summary.setTestCaseSetId(set.getId());
        summary.setTestCaseSetName(set.getName());
        summary.setTestCaseSetVersion(set.getVersion());
        summary.setBusinessZh(set.getBusinessZh());
        summary.setBusinessEn(set.getBusinessEn());
        summary.setTotalCaseCount(testCases.size());

        // 准备脚本文件名列表
        byte[] zipContent = set.getFileContent();
        List<String> scriptFileNames = archiveParseService.parseArchive(zipContent).getScriptFileNames();

        int passed = 0;
        int failed = 0;

        for (TestCase tc : testCases) {
            CaseValidationResult r = new CaseValidationResult();
            r.setCaseNumber(tc.getCaseNumber());
            r.setCaseName(tc.getCaseName());
            r.setBusinessCategory(tc.getBusinessCategory());
            r.setAppName(tc.getAppName());

            // 脚本匹配
            String expectedScript = tc.getCaseNumber() != null ? tc.getCaseNumber().trim() + ".py" : null;
            boolean scriptOk = expectedScript != null && scriptFileNames.contains(expectedScript);
            r.setScriptMatchValid(scriptOk);
            if (scriptOk) {
                r.addValidReasonsItem("脚本[" + expectedScript + "]存在");
            } else {
                r.addInvalidReasonsItem("脚本[" + expectedScript + "]不存在");
            }

            // 预处理规则
            boolean ruleOk = validateRules(set.getBusinessZh(), set.getBusinessEn(), tc.getDependenciesRule(), r);
            r.setPreprocessRuleValid(ruleOk);

            // 软件包
            boolean pkgOk = validatePackages(tc.getDependenciesPackage(), r);
            r.setSoftwarePackageValid(pkgOk);

            boolean overall = scriptOk && ruleOk && pkgOk;
            r.setOverallValid(overall);
            if (overall) { passed++; } else { failed++; }

            summary.addCaseResult(r);
        }

        summary.setPassedCaseCount(passed);
        summary.setFailedCaseCount(failed);
        double rate = testCases.isEmpty() ? 0.0 : (passed * 100.0 / testCases.size());
        summary.setMatchRate(rate);
        return summary;
    }

    private boolean validateRules(String businessZh, String businessEn, String dependenciesRule, CaseValidationResult r) {
        if (dependenciesRule == null || dependenciesRule.trim().isEmpty()) {
            r.addValidReasonsItem("No preprocess rules");
            return true;
        }
        String[] names = dependenciesRule.split(",");
        List<String> missing = new ArrayList<>();
        for (String n : names) {
            String ruleName = n == null ? null : n.trim();
            if (ruleName == null || ruleName.isEmpty()) {
                continue;
            }
            PreprocessRule found = null;
            if (businessZh != null && !businessZh.trim().isEmpty()) {
                found = preprocessRuleDao.findByRuleNameAndBusinessZh(ruleName, businessZh);
            }
            if (found == null && businessEn != null && !businessEn.trim().isEmpty()) {
                found = preprocessRuleDao.findByRuleNameAndBusinessEn(ruleName, businessEn);
            }
            if (found != null) {
                r.addValidReasonsItem("预处理规则[" + ruleName + "]存在");
            } else {
                missing.add(ruleName);
                r.addInvalidReasonsItem("预处理规则[" + ruleName + "]不存在");
            }
        }
        if (missing.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean validatePackages(String dependenciesPackage, CaseValidationResult r) {
        if (dependenciesPackage == null || dependenciesPackage.trim().isEmpty()) {
            r.addValidReasonsItem("No software packages");
            return true;
        }
        String[] names = dependenciesPackage.split(",");
        Set<String> missing = new HashSet<>();
        for (String n : names) {
            String pkg = n == null ? null : n.trim();
            if (pkg == null || pkg.isEmpty()) {
                continue;
            }
            SoftwarePackageInfo info = softwarePackageDao.getSoftwarePackageByName(pkg);
            if (info != null) {
                r.addValidReasonsItem("软件包[" + pkg + "]存在");
            } else {
                missing.add(pkg);
                r.addInvalidReasonsItem("软件包[" + pkg + "]不存在");
            }
        }
        if (missing.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * 触发校验任务（创建任务并异步执行）
     *
     * @param testCaseSetId 用例集ID
     * @return 任务信息（包含taskId和预估时间）
     */
    @Transactional
    public ValidationTaskInfo triggerValidation(Long testCaseSetId) {
        logger.info("triggerValidation called - testCaseSetId: {}", testCaseSetId);
        try {
            // 验证用例集存在
            logger.debug("查找用例集 - testCaseSetId: {}", testCaseSetId);
            TestCaseSet testCaseSet = testCaseSetDao.findById(testCaseSetId);
            if (testCaseSet == null) {
                logger.error("用例集不存在 - testCaseSetId: {}", testCaseSetId);
                throw new IllegalArgumentException("Test case set not found: " + testCaseSetId);
            }
            logger.debug("用例集找到 - testCaseSetId: {}, name: {}", testCaseSetId, testCaseSet.getName());
            
            // 生成任务ID
            String taskId = UUID.randomUUID().toString();
            logger.debug("生成任务ID - taskId: {}, testCaseSetId: {}", taskId, testCaseSetId);
            
            // 创建任务记录
            ValidationTask task = new ValidationTask();
            task.setTestCaseSetId(testCaseSetId);
            task.setTaskId(taskId);
            task.setStatus("PENDING");
            task.setProgress(0);
            task.setCreatedTime(LocalDateTime.now());
            logger.debug("准备插入任务记录 - taskId: {}", taskId);
            validationTaskDao.insert(task);
            logger.debug("任务记录已插入 - taskId: {}", taskId);
            
            // 计算预估时间（根据用例数，每个用例约0.1秒）
            logger.debug("计算预估时间 - testCaseSetId: {}", testCaseSetId);
            long caseCount = testCaseDao.countByTestCaseSetId(testCaseSetId);
            int estimatedTime = Math.max(1, (int)(caseCount / 10)); // 至少1秒
            logger.debug("用例数: {}, 预估时间: {}秒", caseCount, estimatedTime);
            
            // 提交异步任务
            logger.debug("提交异步任务 - testCaseSetId: {}, taskId: {}", testCaseSetId, taskId);
            executeValidationTaskAsync(testCaseSetId, taskId);
            logger.debug("异步任务已提交 - testCaseSetId: {}, taskId: {}", testCaseSetId, taskId);
            
            // 返回任务信息
            ValidationTaskInfo taskInfo = new ValidationTaskInfo();
            taskInfo.setTaskId(taskId);
            taskInfo.setTestCaseSetId(testCaseSetId);
            taskInfo.setStatus("PENDING");
            taskInfo.setEstimatedTime(estimatedTime);
            
            logger.info("triggerValidation completed - testCaseSetId: {}, taskId: {}", testCaseSetId, taskId);
            return taskInfo;
        } catch (Exception e) {
            logger.error("triggerValidation failed - testCaseSetId: {}, error: {}", testCaseSetId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 异步执行校验任务
     */
    @Async
    @Transactional
    public CompletableFuture<Void> executeValidationTaskAsync(Long testCaseSetId, String taskId) {
        try {
            // 更新任务状态为RUNNING，记录开始时间
            LocalDateTime startedTime = LocalDateTime.now();
            validationTaskDao.updateStatus(taskId, "RUNNING", 0, startedTime, null, null);
            
            // 执行完整校验
            ValidationResult result = validateTestCaseSet(testCaseSetId);
            
            // 保存校验结果到数据库（JSON格式）
            String resultJson = objectMapper.writeValueAsString(result);
            validationResultDao.save(testCaseSetId, taskId, resultJson);
            
            // 更新任务状态为COMPLETED，记录完成时间
            LocalDateTime completedTime = LocalDateTime.now();
            validationTaskDao.updateStatus(taskId, "COMPLETED", 100, startedTime, completedTime, null);
            
            logger.info("Validation task completed: taskId={}, testCaseSetId={}", taskId, testCaseSetId);
            
        } catch (Exception e) {
            logger.error("Validation task failed: taskId=" + taskId + ", testCaseSetId=" + testCaseSetId, e);
            // 更新任务状态为FAILED，记录完成时间和错误信息
            LocalDateTime completedTime = LocalDateTime.now();
            validationTaskDao.updateStatus(taskId, "FAILED", null, null, completedTime, 
                e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 获取校验结果（优先从缓存读取，如果没有则查询任务状态）
     *
     * @param testCaseSetId 用例集ID
     * @return 校验结果，如果任务正在执行则返回null
     */
    public ValidationResult getValidationResult(Long testCaseSetId) {
        // 1. 尝试从结果表读取
        String resultJson = validationResultDao.findByTestCaseSetId(testCaseSetId);
        if (resultJson != null) {
            try {
                return objectMapper.readValue(resultJson, ValidationResult.class);
            } catch (Exception e) {
                logger.error("Failed to parse validation result JSON: testCaseSetId=" + testCaseSetId, e);
            }
        }
        
        // 2. 查询任务状态
        ValidationTask task = validationTaskDao.findLatestByTestCaseSetId(testCaseSetId);
        if (task != null && ("PENDING".equals(task.getStatus()) || "RUNNING".equals(task.getStatus()))) {
            // 任务正在执行中，返回null表示需要继续轮询
            return null;
        }
        
        // 3. 没有结果
        return null;
    }
    
    /**
     * 获取任务状态
     *
     * @param testCaseSetId 用例集ID
     * @return 任务状态信息
     */
    public ValidationTask getTaskStatus(Long testCaseSetId) {
        return validationTaskDao.findLatestByTestCaseSetId(testCaseSetId);
    }
    
    /**
     * 任务信息内部类
     */
    public static class ValidationTaskInfo {
        private String taskId;
        private Long testCaseSetId;
        private String status;
        private Integer estimatedTime;
        
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
        public Long getTestCaseSetId() {
            return testCaseSetId;
        }
        
        public void setTestCaseSetId(Long testCaseSetId) {
            this.testCaseSetId = testCaseSetId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public Integer getEstimatedTime() {
            return estimatedTime;
        }
        
        public void setEstimatedTime(Integer estimatedTime) {
            this.estimatedTime = estimatedTime;
        }
    }
}



