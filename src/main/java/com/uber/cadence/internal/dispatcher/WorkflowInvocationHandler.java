/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.uber.cadence.internal.dispatcher;

import com.google.common.base.Defaults;
import com.uber.cadence.DataConverter;
import com.uber.cadence.StartWorkflowOptions;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionCompletedEventAttributes;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.common.FlowHelpers;
import com.uber.cadence.common.WorkflowExecutionUtils;
import com.uber.cadence.generic.GenericWorkflowClientExternal;
import com.uber.cadence.generic.StartWorkflowExecutionParameters;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

class WorkflowInvocationHandler implements InvocationHandler {

    private static final ThreadLocal<AtomicReference<WorkflowExternalResult>> asyncResult = new ThreadLocal<>();
    private final GenericWorkflowClientExternal genericClient;
    private final StartWorkflowOptions options;
    private final DataConverter dataConverter;

    public static void initAsyncInvocation() {
        if (asyncResult.get() != null) {
            throw new IllegalStateException("already in async invocation");
        }
        asyncResult.set(new AtomicReference<>());
    }

    public static WorkflowExternalResult getAsyncInvocationResult() {
        try {
            AtomicReference<WorkflowExternalResult> reference = asyncResult.get();
            if (reference == null) {
                throw new IllegalStateException("initAsyncInvocation wasn't called");
            }
            WorkflowExternalResult result = reference.get();
            if (result == null) {
                throw new IllegalStateException("async result wasn't set");
            }
            return result;
        } finally {
            asyncResult.remove();
        }
    }

    WorkflowInvocationHandler(GenericWorkflowClientExternal genericClient, StartWorkflowOptions options, DataConverter dataConverter) {
        this.genericClient = genericClient;
        this.options = options;
        this.dataConverter = dataConverter;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        WorkflowMethod workflowMethod = method.getAnnotation(WorkflowMethod.class);
        if (workflowMethod == null) {
            throw new IllegalArgumentException(method.getName() + " is not annotated with @WorkflowMethod");
        }
        String workflowName = workflowMethod.name();
        if (workflowName.isEmpty()) {
            workflowName = FlowHelpers.getSimpleName(method);
        }
        StartWorkflowExecutionParameters parameters = new StartWorkflowExecutionParameters();
        parameters.setExecutionStartToCloseTimeoutSeconds(options.getExecutionStartToCloseTimeoutSeconds());
        parameters.setTaskList(options.getTaskList());
        parameters.setTaskStartToCloseTimeoutSeconds(options.getTaskStartToCloseTimeoutSeconds());
        parameters.setWorkflowType(new WorkflowType().setName(workflowName));
        parameters.setWorkflowId(UUID.randomUUID().toString()); // TODO: specifying id.
        byte[] input = dataConverter.toData(args);
        parameters.setInput(input);
        // TODO: Return workflow result or its execution through async.
        WorkflowExecution execution = genericClient.startWorkflow(parameters);
        // TODO: Wait for result using long poll Cadence API.
        WorkflowService.Iface service = genericClient.getService();
        String domain = genericClient.getDomain();
        WorkflowExternalResult result = new WorkflowExternalResult(
                service,
                domain,
                execution,
                options.getExecutionStartToCloseTimeoutSeconds(),
                dataConverter,
                method.getReturnType());
        AtomicReference<WorkflowExternalResult> async = asyncResult.get();
        if (async != null) {
            async.set(result);
            return Defaults.defaultValue(method.getReturnType());
        }
        return result.getResult();
    }
}
