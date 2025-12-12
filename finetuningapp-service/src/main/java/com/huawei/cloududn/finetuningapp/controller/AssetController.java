package com.huawei.cloududn.finetuningapp.controller;

import com.huawei.cloududn.finetuningapp.api.AssetsApi;
import com.huawei.cloududn.finetuningapp.api.model.*;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

/**
 * 资产管理Controller - 实现AssetsApi接口
 *
 * @author FinetuningApp Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@Api(tags = "资产管理")
public class AssetController implements AssetsApi {

    @Override
    public ResponseEntity<ModelVersionListResponse> listModelVersions(
            String modelType, Boolean isActive, Integer page, Integer size) {
        log.info("List model versions: modelType={}, isActive={}, page={}, size={}",
                modelType, isActive, page, size);

        // TODO: 实现模型版本列表查询逻辑
        ModelVersionListResponse response = new ModelVersionListResponse();
        response.setContent(new ArrayList<>());
        response.setTotalElements(0L);
        response.setTotalPages(0);
        response.setSize(size != null ? size : 20);
        response.setNumber(page != null ? page : 0);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SampleStatisticsResponse> getSampleStatistics(
            String appType, Long dialingTaskId, Long startTime, Long endTime) {
        log.info("Get sample statistics: appType={}, dialingTaskId={}, startTime={}, endTime={}",
                appType, dialingTaskId, startTime, endTime);

        // TODO: 实现样本统计查询逻辑
        SampleStatisticsResponse response = new SampleStatisticsResponse();
        response.setStatistics(new ArrayList<>());

        return ResponseEntity.ok(response);
    }
}
