/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.UeDao;
import com.huawei.cloududn.dialingtest.model.Executor;
import com.huawei.cloududn.dialingtest.model.Ue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.Duration;

/**
 * 执行机选择服务,负责选择可用的执行机和UE
 * 支持负载均衡、健康度评估、优先级排序等选择策略
 *
 * @author g00940940
 * @since 2025-11-09
 */
@Service
public class ExecutorSelectionService {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorSelectionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    @Autowired
    private ExecutorDao executorDao;

    @Autowired
    private UeDao ueDao;

    /**
     * 选择一个可用的执行机和UE
     * 使用智能选择算法：综合考虑健康度、负载均衡、优先级排序
     *
     * @return 执行机和UE信息,如果没有可用的返回null
     */
    public ExecutorUeInfo selectIdleExecutorAndUe() {
        logger.debug("Starting intelligent executor and UE selection");

        // 获取所有在线执行机
        List<Executor> onlineExecutors = executorDao.findPage(1, null, 0, 100);
        if (onlineExecutors == null || onlineExecutors.isEmpty()) {
            logger.warn("No online executors found");
            return null;
        }

        logger.debug("Found {} online executors, evaluating candidates", onlineExecutors.size());

        // 评估所有候选执行机
        List<ExecutorCandidate> candidates = onlineExecutors.stream()
            .map(this::evaluateExecutorCandidate)
            .filter(candidate -> !candidate.getIdleUes().isEmpty())
            .sorted(Comparator.comparingDouble(ExecutorCandidate::getPriorityScore).reversed())
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            logger.warn("No executors with idle UEs found");
            return null;
        }

        // 记录候选执行机信息
        for (int i = 0; i < candidates.size(); i++) {
            ExecutorCandidate candidate = candidates.get(i);
            logger.debug("Candidate {}: executor={}, idleUEs={}, healthScore={:.2f}, " +
                    "loadScore={:.2f}, priorityScore={:.2f}",
                i + 1, candidate.getExecutor().getName(), candidate.getIdleUes().size(),
                candidate.getHealthScore(), candidate.getLoadScore(), candidate.getPriorityScore());
        }

        // 选择最佳候选执行机
        ExecutorCandidate bestCandidate = candidates.get(0);

        // 从最佳候选执行机的空闲UE中选择一个
        Ue selectedUe = selectUeFromCandidate(bestCandidate);

        if (selectedUe != null) {
            logger.info("Selected executor={}, ue={}, priorityScore={:.2f}",
                bestCandidate.getExecutor().getName(), selectedUe.getMsisdn(), bestCandidate.getPriorityScore());
            return new ExecutorUeInfo(bestCandidate.getExecutor(), selectedUe);
        } else {
            logger.warn("Failed to select UE from best candidate: {}", bestCandidate.getExecutor().getName());
            return null;
        }
    }

    /**
     * 评估执行机候选人
     */
    private ExecutorCandidate evaluateExecutorCandidate(Executor executor) {
        List<Ue> allUes = ueDao.findByExecutorName(executor.getName());
        List<Ue> idleUes = allUes.stream()
            .filter(this::isUeIdle)
            .collect(Collectors.toList());

        return new ExecutorCandidate(executor, idleUes);
    }

    /**
     * 从候选执行机中选择一个UE
     * 在多个空闲UE中随机选择，避免总是选择同一个UE
     */
    private Ue selectUeFromCandidate(ExecutorCandidate candidate) {
        List<Ue> idleUes = candidate.getIdleUes();
        if (idleUes.isEmpty()) {
            return null;
        }

        // 在空闲UE中随机选择一个
        int randomIndex = random.nextInt(idleUes.size());
        return idleUes.get(randomIndex);
    }

    /**
     * 检查UE是否空闲
     *
     * @param ue UE实体
     * @return true if idle
     */
    private boolean isUeIdle(Ue ue) {
        if (ue.getInfo() == null || ue.getInfo().isEmpty()) {
            return false;
        } else {
            try {
                JsonNode infoNode = objectMapper.readTree(ue.getInfo());
                String status = infoNode.has("status") ? infoNode.get("status").asText("") : "";
                return "idle".equalsIgnoreCase(status);
            } catch (Exception e) {
                logger.warn("Failed to parse UE info for msisdn={}", ue.getMsisdn(), e);
                return false;
            }
        }
    }

    /**
     * 执行机候选信息，包含健康度和优先级评分
     */
    private static class ExecutorCandidate {
        private final Executor executor;
        private final List<Ue> idleUes;
        private final double healthScore;
        private final double loadScore;

        public ExecutorCandidate(Executor executor, List<Ue> idleUes) {
            this.executor = executor;
            this.idleUes = idleUes;
            this.healthScore = calculateHealthScore(executor);
            this.loadScore = calculateLoadScore(idleUes.size());
        }

        /**
         * 计算执行机健康度评分 (0.0-1.0)
         * 考虑在线时间、连接稳定性等因素
         */
        private double calculateHealthScore(Executor executor) {
            double score = 0.5; // 基础分数

            // 在线时间评分：在线时间越长，分数越高
            if (executor.getLastOnlineTime() != null) {
                try {
                    // 假设lastOnlineTime是String类型，需要解析为Instant
                    Instant lastOnline = Instant.parse(executor.getLastOnlineTime().toString());
                    Duration onlineDuration = Duration.between(lastOnline, Instant.now());
                    if (onlineDuration.toHours() > 24) {
                        score += 0.3; // 连续在线超过24小时，加分
                    } else if (onlineDuration.toHours() > 1) {
                        score += 0.2; // 连续在线超过1小时，加分
                    }
                } catch (Exception e) {
                    // 如果解析失败，使用基础分数
                    logger.debug("Failed to parse lastOnlineTime for executor: {}", executor.getName(), e);
                }
            }

            // IP地址评分：有IP地址的执行机更可信
            if (executor.getIp() != null && !executor.getIp().isEmpty()) {
                score += 0.1;
            }

            // 确保分数在合理范围内
            return Math.max(0.0, Math.min(1.0, score));
        }

        /**
         * 计算负载评分 (0.0-1.0)
         * 空闲UE数量越多，负载越轻，分数越高
         */
        private double calculateLoadScore(int idleUeCount) {
            if (idleUeCount >= 5) {
                return 1.0; // 很多空闲UE，轻负载
            } else if (idleUeCount >= 3) {
                return 0.8; // 有一些空闲UE，中等负载
            } else if (idleUeCount >= 1) {
                return 0.6; // 只有一个空闲UE，重负载
            } else {
                return 0.0; // 没有空闲UE，超载
            }
        }

        /**
         * 计算综合优先级评分
         */
        public double getPriorityScore() {
            return (healthScore * 0.6) + (loadScore * 0.4);
        }

        public Executor getExecutor() { return executor; }
        public List<Ue> getIdleUes() { return idleUes; }
        public double getHealthScore() { return healthScore; }
        public double getLoadScore() { return loadScore; }
    }

    /**
     * 执行机和UE信息封装类
     */
    public static class ExecutorUeInfo {
        private final Executor executor;
        private final Ue ue;

        public ExecutorUeInfo(Executor executor, Ue ue) {
            this.executor = executor;
            this.ue = ue;
        }

        public Executor getExecutor() {
            return executor;
        }

        public Ue getUe() {
            return ue;
        }
    }
}

