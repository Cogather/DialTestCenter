/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.Locale;

import javax.annotation.PreDestroy;

/**
 * V6全局线程池配置类
 * 统一管理双队列消息发送任务的线程池
 * 实现真正的并行处理,提高消息发送效率
 *
 * @author g00940940
 * @since 2025-11-20
 */
@Configuration
public class ThreadPoolConfig {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolConfig.class);

    @Value("${websocket.thread-pool.core-size:10}")
    private int corePoolSize;

    @Value("${websocket.thread-pool.max-size:50}")
    private int maxPoolSize;

    @Value("${websocket.thread-pool.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${websocket.thread-pool.keep-alive-seconds:60}")
    private long keepAliveSeconds;

    private ExecutorService globalExecutorService;

    /**
     * 创建全局共享线程池
     * 用于处理双队列消息发送任务
     *
     * @return ExecutorService实例
     */
    @Bean(name = "websocketMessageSenderExecutor")
    public ExecutorService getExecutorService() {
        if (globalExecutorService == null) {
            synchronized (this) {
                if (globalExecutorService == null) {
                    globalExecutorService = createThreadPool();
                    logger.info("Global executor service created: coreSize={}, maxSize={}, queueCapacity={}",
                            corePoolSize, maxPoolSize, queueCapacity);
                }
            }
        }
        return globalExecutorService;
    }

    /**
     * 创建线程池实例
     *
     * @return ThreadPoolExecutor实例
     */
    private ExecutorService createThreadPool() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("websocket-sender-" + thread.getId());
                    thread.setDaemon(false);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * 获取线程池统计信息
     *
     * @return 统计信息字符串
     */
    public String getThreadPoolStats() {
        if (globalExecutorService == null) {
            return "Thread pool not initialized";
        }
        if (!(globalExecutorService instanceof ThreadPoolExecutor)) {
            return "Thread pool type not supported";
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) globalExecutorService;
        return String.format(Locale.ROOT,
                "ThreadPool[active=%d, poolSize=%d, coreSize=%d, maxSize=%d, queueSize=%d, completedTasks=%d]",
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount()
        );
    }

    /**
     * 应用关闭时优雅关闭线程池
     */
    @PreDestroy
    public void shutdown() {
        if (globalExecutorService != null) {
            logger.info("Shutting down global executor service: {}", getThreadPoolStats());
            globalExecutorService.shutdown();
            try {
                if (!globalExecutorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("Executor service did not terminate in time, forcing shutdown");
                    globalExecutorService.shutdownNow();
                    if (!globalExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                        logger.error("Executor service did not terminate after forced shutdown");
                    }
                } else {
                    logger.info("Global executor service shutdown completed");
                }
            } catch (InterruptedException e) {
                logger.warn("Shutdown interrupted, forcing shutdown", e);
                globalExecutorService.shutdownNow();
            }
        }
    }
}

