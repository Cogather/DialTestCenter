/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement;

import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtest.model.Executor;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.SessionBindingRegistry;
import com.huawei.cloududn.dialingtest.api.ExecutorsApi;
import com.huawei.cloududn.dialingtest.model.ExecutorPageResponse;
import com.huawei.cloududn.dialingtest.model.ExecutorPageData;
import com.huawei.cloududn.dialingtest.model.OperationResponse;
import com.huawei.cloududn.dialingtest.model.RefreshExecutorRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

/**
 * Executor northbound REST API.
 *
 * <p>Provides executor list query and refresh command.</p>
 *
 * @author g00940940
 * @since 2025-11-04
 */
@RestController
@RequestMapping("/api")
public class ExecutorController implements ExecutorsApi {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorController.class);

    @Autowired
    private ExecutorDao executorDao;

    @Autowired
    private SessionBindingRegistry registry;

    @Override
    public ResponseEntity<ExecutorPageResponse> listExecutors(
            Integer page, Integer size, Integer status, String keyword) {
        logger.info("Listing executors, page={}, size={}, status={}, keyword={}",
                page, size, status, keyword);
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        if (p < 0 || s <= 0 || s > 200) {
            logger.warn("Invalid page parameters, page={}, size={}", p, s);
            ExecutorPageResponse resp = new ExecutorPageResponse();
            resp.setSuccess(false);
            resp.setMessage("Invalid page parameters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }
        Integer st = status == null ? null : status;
        logger.debug("Querying executors from database, offset={}, limit={}", p * s, s);
        List<Executor> content = executorDao.findPage(st, keyword, p * s, s);
        int total = executorDao.count(st, keyword);
        logger.debug("Query completed, found {} executors, total count={}", content.size(), total);
        int totalPages = (int) Math.ceil((double) total / s);

        ExecutorPageData data = new ExecutorPageData();
        data.setContent(content);
        data.setTotalElements(total);
        data.setTotalPages(totalPages);
        data.setSize(s);
        data.setNumber(p);
        data.setFirst(p == 0);
        data.setLast(p >= totalPages - 1);

        ExecutorPageResponse resp = new ExecutorPageResponse();
        resp.setSuccess(true);
        resp.setMessage("OK");
        resp.setData(data);
        logger.info("Listing executors completed successfully, returned {} executors", content.size());
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<OperationResponse> refreshExecutor(
            String xCsrfToken,
            String xUsername,
            RefreshExecutorRequest refreshExecutorRequest) {
        String name = refreshExecutorRequest == null ? null : refreshExecutorRequest.getName();
        logger.info("Refresh executor request received for: {} by user: {}", name, xUsername);

        try {
            // 验证用户名
            if (xUsername == null || xUsername.trim().isEmpty()) {
                logger.warn("Refresh executor request missing username");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(op(false, "Missing username"));
            }

            // 验证CSRF令牌
            if (xCsrfToken == null || xCsrfToken.trim().isEmpty()) {
                logger.warn("Refresh executor request missing CSRF token");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(op(false, "Missing CSRF token"));
            }

            if (name == null || name.trim().isEmpty()) {
                logger.warn("Refresh executor request missing executor name");
                return ResponseEntity.badRequest()
                    .body(op(false, "Missing executor name"));
            }

            // 检查执行机是否存在
            Executor executor = executorDao.findByName(name);
            if (executor == null) {
                logger.warn("Executor not found: {}", name);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(op(false, "Executor not found: " + name));
            }

            // 检查执行机是否在线
            if (!"ONLINE".equals(executor.getStatus()) && executor.getStatus() != 1) {
                logger.warn("Executor is not online: {} (status: {})", name, executor.getStatus());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(op(false, "Executor is not online: " + name));
            }

            // 由于当前通信协议中没有专门的刷新消息类型，
            // 执行机信息刷新主要通过心跳机制（Report-Msg）自动进行
            // 这里我们记录刷新请求，并在下次心跳时可以特殊处理
            logger.info("Executor refresh request acknowledged for: {} by user: {}. " +
                    "Info will be updated on next heartbeat.",
                name, xUsername);

            // 返回成功，实际刷新通过心跳机制进行
            return ResponseEntity.ok(op(true,
                    "Refresh request acknowledged. Executor info will be updated on next heartbeat."));

        } catch (Exception e) {
            logger.error("Failed to process refresh executor request for: {} by user: {}", name, xUsername, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(op(false, "Internal server error: " + e.getMessage()));
        }
    }

    private static OperationResponse op(boolean success, String message) {
        OperationResponse r = new OperationResponse();
        r.setSuccess(success);
        r.setMessage(message);
        return r;
    }
}


