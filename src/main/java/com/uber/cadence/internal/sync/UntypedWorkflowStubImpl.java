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
package com.uber.cadence.internal.sync;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionAlreadyStartedError;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.client.DuplicateWorkflowException;
import com.uber.cadence.client.UntypedWorkflowStub;
import com.uber.cadence.client.WorkflowFailureException;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.internal.common.StartWorkflowExecutionParameters;
import com.uber.cadence.internal.common.WorkflowExecutionFailedException;
import com.uber.cadence.internal.common.WorkflowExecutionUtils;
import com.uber.cadence.internal.external.GenericWorkflowClientExternal;
import com.uber.cadence.internal.replay.QueryWorkflowParameters;
import com.uber.cadence.internal.replay.SignalExternalWorkflowParameters;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

class UntypedWorkflowStubImpl implements UntypedWorkflowStub {
    private final GenericWorkflowClientExternal genericClient;
    private final DataConverter dataConverter;
    private final String workflowType;
    private AtomicReference<WorkflowExecution> execution = new AtomicReference<>();
    private final WorkflowOptions options;

    UntypedWorkflowStubImpl(GenericWorkflowClientExternal genericClient, DataConverter dataConverter,
                            WorkflowExecution execution) {
        this.genericClient = genericClient;
        this.dataConverter = dataConverter;
        this.workflowType = null;
        this.execution.set(execution);
        this.options = null;
    }

    UntypedWorkflowStubImpl(GenericWorkflowClientExternal genericClient, DataConverter dataConverter,
                            String workflowType, WorkflowOptions options) {
        this.genericClient = genericClient;
        this.dataConverter = dataConverter;
        this.workflowType = workflowType;
        this.options = options;
    }

    @Override
    public void signal(String signalName, Object... input) {
        checkStarted();
        SignalExternalWorkflowParameters p = new SignalExternalWorkflowParameters();
        p.setInput(dataConverter.toData(input));
        p.setSignalName(signalName);
        p.setWorkflowId(execution.get().getWorkflowId());
        // TODO: Deal with signalling started workflow only, when requested
        // Commented out to support signalling workflows that called continue as new.
//        p.setRunId(execution.getRunId());
        genericClient.signalWorkflowExecution(p);
    }

    WorkflowExecution startWithOptions(WorkflowOptions o, Object... args) {
        if (o == null) {
            throw new IllegalStateException("UntypedWorkflowStub wasn't created through " +
                    "WorkflowClient::newUntypedWorkflowStub(String workflowType, WorkflowOptions options)");
        }
        if (execution.get() != null) {
            throw new IllegalStateException("already started for execution=" + execution.get());
        }
        StartWorkflowExecutionParameters p = new StartWorkflowExecutionParameters();
        p.setTaskStartToCloseTimeoutSeconds(o.getTaskStartToCloseTimeout().getSeconds());
        if (o.getWorkflowId() == null) {
            p.setWorkflowId(UUID.randomUUID().toString());
        } else {
            p.setWorkflowId(o.getWorkflowId());
        }
        p.setExecutionStartToCloseTimeoutSeconds(o.getExecutionStartToCloseTimeout().getSeconds());
        p.setInput(dataConverter.toData(args));
        p.setWorkflowType(new WorkflowType().setName(workflowType));
        p.setTaskList(o.getTaskList());
        p.setChildPolicy(o.getChildPolicy());
        try {
            execution.set(genericClient.startWorkflow(p));
        } catch (WorkflowExecutionAlreadyStartedError e) {
            WorkflowExecution execution = new WorkflowExecution()
                    .setWorkflowId(p.getWorkflowId())
                    .setRunId(e.getRunId());
            throw new DuplicateWorkflowException(execution, workflowType, e.getMessage());
        }
        return execution.get();
    }

    @Override
    public WorkflowExecution start(Object... args) {
        return startWithOptions(WorkflowOptions.merge(null, options), args);
    }

    @Override
    public <R> R getResult(Class<R> returnType) {
        try {
            return getResult(Long.MAX_VALUE, TimeUnit.MILLISECONDS, returnType);
        } catch (TimeoutException e) {
            throw CheckedExceptionWrapper.throwWrapped(e);
        }
    }

    @Override
    public <R> R getResult(long timeout, TimeUnit unit, Class<R> returnType) throws TimeoutException {
        checkStarted();
        try {
            byte[] resultValue = WorkflowExecutionUtils.getWorkflowExecutionResult(
                    genericClient.getService(), genericClient.getDomain(), execution.get(), workflowType, timeout, unit);
            if (resultValue == null) {
                return null;
            }
            return dataConverter.fromData(resultValue, returnType);
        } catch (WorkflowExecutionFailedException e) {
            Class<Throwable> detailsClass;
            try {
                @SuppressWarnings("unchecked")
                Class<Throwable> dc = (Class<Throwable>) Class.forName(e.getReason());
                detailsClass = dc;
            } catch (Exception ee) {
                RuntimeException failure = new RuntimeException("Couldn't deserialize failure cause " +
                        "as the reason field is expected to contain an exception class name", e);
                throw new WorkflowFailureException(execution.get(), workflowType, e.getDecisionTaskCompletedEventId(), failure);
            }
            Throwable cause = dataConverter.fromData(e.getDetails(), detailsClass);
            throw new WorkflowFailureException(execution.get(), workflowType, e.getDecisionTaskCompletedEventId(), cause);
        }
    }


    @Override
    public <R> R query(String queryType, Class<R> returnType, Object... args) {
        checkStarted();
        QueryWorkflowParameters p = new QueryWorkflowParameters();
        p.setInput(dataConverter.toData(args));
        p.setQueryType(queryType);
        p.setWorkflowId(execution.get().getWorkflowId());
        byte[] result = genericClient.queryWorkflow(p);
        return dataConverter.fromData(result, returnType);
    }

    @Override
    public void cancel() {
        if (execution.get() == null || execution.get().getWorkflowId() == null) {
            return;
        }
        genericClient.requestCancelWorkflowExecution(execution.get());
    }

    private void checkStarted() {
        if (execution.get() == null || execution.get().getWorkflowId() == null) {
            throw new IllegalStateException("Null workflowId. Was workflow started?");
        }
    }
}
