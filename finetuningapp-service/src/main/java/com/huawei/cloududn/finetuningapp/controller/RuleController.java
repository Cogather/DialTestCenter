package com.huawei.cloududn.finetuningapp.controller;

import com.huawei.cloududn.finetuningapp.api.ConfigsApi;
import com.huawei.cloududn.finetuningapp.api.model.CleaningRule;
import com.huawei.cloududn.finetuningapp.api.model.CleaningRuleUpdateRequest;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 规则配置Controller - 实现ConfigsApi接口
 *
 * @author FinetuningApp Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@Api(tags = "规则配置")
public class RuleController implements ConfigsApi {

    @Override
    public ResponseEntity<CleaningRule> getDefaultRule(String appType) {
        log.info("Get default rule: appType={}", appType);

        // TODO: 实现默认规则查询逻辑
        CleaningRule rule = new CleaningRule();
        rule.setId(1L);
        rule.setName("Default Rule");
        rule.setAppType(appType);
        rule.setIsDefault(true);

        return ResponseEntity.ok(rule);
    }

    @Override
    public ResponseEntity<Void> updateDefaultRule(@Valid CleaningRuleUpdateRequest body) {
        log.info("Update default rule: appType={}", body.getAppType());

        // TODO: 实现规则更新逻辑

        return ResponseEntity.ok().build();
    }
}
