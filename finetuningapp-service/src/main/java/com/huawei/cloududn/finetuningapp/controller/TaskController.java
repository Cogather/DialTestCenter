package com.huawei.cloududn.finetuningapp.controller;

import com.huawei.cloududn.finetuningapp.api.TasksApi;
import com.huawei.cloududn.finetuningapp.api.model.*;
import com.huawei.cloududn.finetuningapp.common.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 任务管理Controller - 实现TasksApi接口
 *
 * @author FinetuningApp Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@Api(tags = "任务管理")
public class TaskController implements TasksApi {

    @Override
    public ResponseEntity<TaskResponse> submitPreprocessTask(@Valid PreprocessTaskRequest body) {
        log.info("Received preprocess task request: dialingTaskId={}, ueId={}",
                body.getDialingTaskId(), body.getUeId());

        // TODO: 实现预处理任务提交逻辑
        TaskResponse response = new TaskResponse();
        response.setJobId(1001L);
        response.setStatus("PENDING");

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TaskResponse> submitTrainTask(@Valid TrainTaskRequest body) {
        log.info("Received train task request: triggerSource={}, modelType={}",
                body.getTriggerSource(), body.getModelType());

        // TODO: 实现训练任务提交逻辑
        TaskResponse response = new TaskResponse();
        response.setJobId(2001L);
        response.setStatus("PENDING");

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TaskResponse> submitReplayTask(@Valid ReplayTaskRequest body) {
        log.info("Received replay task request: modelVersion={}, datasetScope={}",
                body.getModelVersion(), body.getDatasetScope());

        // TODO: 实现回放任务提交逻辑
        TaskResponse response = new TaskResponse();
        response.setJobId(3001L);
        response.setStatus("PENDING");

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TaskDetail> getTaskDetail(Long taskId) {
        log.info("Query task detail: taskId={}", taskId);

        // TODO: 实现任务详情查询逻辑
        TaskDetail detail = new TaskDetail();
        detail.setId(taskId);
        detail.setJobType("PREPROCESS");
        detail.setStatus("RUNNING");

        return ResponseEntity.ok(detail);
    }

    @Override
    public ResponseEntity<Void> stopTask(Long taskId) {
        log.info("Stop task: taskId={}", taskId);

        // TODO: 实现任务终止逻辑

        return ResponseEntity.ok().build();
    }
}
