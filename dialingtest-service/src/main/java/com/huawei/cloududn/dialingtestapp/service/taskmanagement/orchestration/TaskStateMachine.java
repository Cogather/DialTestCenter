/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration;

import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.TaskContext;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.action.TaskAction;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.listener.TaskStateChangeListener;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskEvent;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;

/**
 * 任务状态机，负责管理状态转换和动作执行
 *
 * @author g00940940
 * @since 2025-11-10
 */
@Component
public class TaskStateMachine {
    private static final Logger logger = LoggerFactory.getLogger(TaskStateMachine.class);
    private Map<StateEventKey, TaskState> transitionTable;
    private Map<TaskState, TaskAction> stateActions;
    private List<TaskStateChangeListener> listeners;

    @PostConstruct
    public void init() {
        initTransitionTable();
        initActionTable();
        initListeners();
        logger.info("TaskStateMachine initialized with {} transitions and {} state actions", 
                transitionTable.size(), stateActions.size());
    }

    /**
     * 注册状态变更监听器
     *
     * @param listener 监听器
     */
    public void addListener(TaskStateChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        } else {
            logger.debug("Listener already registered or null");
        }
    }

    /**
     * 注册状态动作
     *
     * @param state 状态
     * @param action 动作
     */
    public void registerAction(TaskState state, TaskAction action) {
        if (state != null && action != null) {
            stateActions.put(state, action);
            logger.debug("Registered action for state: {}", state);
        } else {
            logger.warn("Cannot register null state or action");
        }
    }

    /**
     * 发送事件驱动状态转换
     *
     * @param current 当前状态
     * @param event 事件
     * @param context 任务上下文
     * @return 新状态
     */
    public TaskState sendEvent(TaskState current, TaskEvent event, TaskContext context) {
        if (current == null) {
            logger.warn("Current state is null, cannot send event");
            return null;
        } else if (event == null) {
            logger.warn("Event is null for state: {}", current);
            return current;
        } else if (context == null) {
            logger.warn("Context is null for state: {} and event: {}", current, event);
            return current;
        } else {
            StateEventKey key = new StateEventKey(current, event);
            TaskState nextState = transitionTable.get(key);
            if (nextState == null) {
                logger.warn("No transition defined for state: {} and event: {}", current, event);
                return current;
            } else {
                logger.info("State transition: {} -[{}]-> {}", current, event, nextState);
                notifyListeners(current, nextState, context);
                executeAction(nextState, context);
                return nextState;
            }
        }
    }

    private void initTransitionTable() {
        transitionTable = new HashMap<StateEventKey, TaskState>();
        transitionTable.put(key(TaskState.START_VALIDATION, TaskEvent.TASK_SUCCESS), TaskState.FINAL);
        transitionTable.put(key(TaskState.START_VALIDATION, TaskEvent.TASK_FAILED),
                TaskState.START_TRAINING_DIALING);
        transitionTable.put(key(TaskState.START_TRAINING_DIALING, TaskEvent.TASK_SUCCESS),
                TaskState.START_MODEL_TRAIN);
        transitionTable.put(key(TaskState.START_TRAINING_DIALING, TaskEvent.TASK_FAILED),
                TaskState.START_TRAINING_DIALING);
        transitionTable.put(key(TaskState.START_MODEL_TRAIN, TaskEvent.TASK_SUCCESS), TaskState.START_MODEL_REPLAY);
        transitionTable.put(key(TaskState.START_MODEL_TRAIN, TaskEvent.TASK_FAILED), TaskState.FINAL);
        transitionTable.put(key(TaskState.START_MODEL_REPLAY, TaskEvent.TASK_SUCCESS), TaskState.START_GRAY_VALIDATION);
        transitionTable.put(key(TaskState.START_MODEL_REPLAY, TaskEvent.TASK_FAILED), TaskState.FINAL);
        transitionTable.put(key(TaskState.START_GRAY_VALIDATION, TaskEvent.TASK_SUCCESS), TaskState.START_FULL_REPLAY);
        transitionTable.put(key(TaskState.START_GRAY_VALIDATION, TaskEvent.TASK_FAILED), TaskState.FINAL);
        transitionTable.put(key(TaskState.START_FULL_REPLAY, TaskEvent.TASK_SUCCESS), TaskState.START_WHITELIST);
        transitionTable.put(key(TaskState.START_FULL_REPLAY, TaskEvent.TASK_FAILED), TaskState.FINAL);
        transitionTable.put(key(TaskState.START_WHITELIST, TaskEvent.TASK_SUCCESS), TaskState.START_FULL_RELEASE);
        transitionTable.put(key(TaskState.START_WHITELIST, TaskEvent.TASK_FAILED), TaskState.FINAL);
        transitionTable.put(key(TaskState.START_FULL_RELEASE, TaskEvent.TASK_SUCCESS), TaskState.FINAL);
        transitionTable.put(key(TaskState.START_FULL_RELEASE, TaskEvent.TASK_FAILED), TaskState.FINAL);
    }

    private void initActionTable() {
        stateActions = new HashMap<TaskState, TaskAction>();
    }

    private void initListeners() {
        listeners = new ArrayList<TaskStateChangeListener>();
    }

    private StateEventKey key(TaskState state, TaskEvent event) {
        return new StateEventKey(state, event);
    }

    private void notifyListeners(TaskState from, TaskState to, TaskContext context) {
        for (TaskStateChangeListener listener : listeners) {
            try {
                listener.onStateChanged(from, to, context);
            } catch (Exception e) {
                logger.error("Listener notification failed for state change: {} -> {}", from, to, e);
            }
        }
    }

    private void executeAction(TaskState state, TaskContext context) {
        if (state == TaskState.FINAL) {
            logger.debug("Reached FINAL state, no action to execute");
            return;
        } else {
            TaskAction action = stateActions.get(state);
            if (action != null) {
                try {
                    logger.debug("Executing action for state: {}", state);
                    action.execute(context);
                } catch (Exception e) {
                    logger.error("Action execution failed for state: {}", state, e);
                }
            } else {
                logger.debug("No action defined for state: {}", state);
            }
        }
    }

    /**
     * 状态事件键，用于状态转换表查找
     */
    private static class StateEventKey {
        private final TaskState state;
        private final TaskEvent event;

        public StateEventKey(TaskState state, TaskEvent event) {
            this.state = state;
            this.event = event;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else {
                if (o == null || getClass() != o.getClass()) {
                    return false;
                } else {
                    StateEventKey that = (StateEventKey) o;
                    return state == that.state && event == that.event;
                }
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(state, event);
        }
    }
}

