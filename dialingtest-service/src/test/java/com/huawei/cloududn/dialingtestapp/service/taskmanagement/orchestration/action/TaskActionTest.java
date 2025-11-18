/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.action;

import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.TaskContext;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskState;

import org.junit.Assert;
import org.junit.Test;

/**
 * TaskAction 接口行为校验
 *
 * @author g00940940
 * @since 2025-11-18
 */
public class TaskActionTest {
    @Test
    public void testExecute_LambdaUpdatesContext() {
        TaskContext context = new TaskContext();
        TaskAction action = ctx -> ctx.getData().put("key", "value");

        action.execute(context);

        Assert.assertEquals("value", context.getData().get("key"));
    }

    @Test
    public void testExecute_CustomImplementationUpdatesStep() {
        TaskContext context = new TaskContext();
        TaskAction action = new TaskAction() {
            @Override
            public void execute(TaskContext ctx) {
                ctx.setStep(TaskState.START_VALIDATION);
            }
        };

        action.execute(context);

        Assert.assertEquals(TaskState.START_VALIDATION, context.getStep());
    }
}

