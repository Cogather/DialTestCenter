package com.huawei.cloududn.finetuningapp.controller;

import com.huawei.cloududn.finetuningapp.api.InternalApi;
import com.huawei.cloududn.finetuningapp.api.model.AlgoCallbackRequest;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 内部回调Controller - 实现InternalApi接口
 * 用于接收外部系统（如C++算力服务）的回调
 *
 * @author FinetuningApp Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@Api(tags = "内部回调")
public class CallbackController implements InternalApi {

    @Override
    public ResponseEntity<Void> handleAlgoCallback(@Valid AlgoCallbackRequest body) {
        log.info("Received algo callback: jobId={}, status={}", body.getJobId(), body.getStatus());

        // TODO: 实现算力服务回调处理逻辑

        return ResponseEntity.ok().build();
    }
}
