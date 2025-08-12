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

package com.uber.cadence.internal.external;

import com.google.common.base.Strings;
import com.uber.cadence.Header;
import com.uber.cadence.Memo;
import com.uber.cadence.QueryWorkflowRequest;
import com.uber.cadence.QueryWorkflowResponse;
import com.uber.cadence.RequestCancelWorkflowExecutionRequest;
import com.uber.cadence.RetryPolicy;
import com.uber.cadence.SearchAttributes;
import com.uber.cadence.SignalWithStartWorkflowExecutionAsyncRequest;
import com.uber.cadence.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.SignalWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionAsyncRequest;
import com.uber.cadence.StartWorkflowExecutionAsyncResponse;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionResponse;
import com.uber.cadence.TaskList;
import com.uber.cadence.TerminateWorkflowExecutionRequest;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionAlreadyStartedError;
import com.uber.cadence.WorkflowQuery;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.internal.common.*;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.replay.QueryWorkflowParameters;
import com.uber.cadence.internal.replay.SignalExternalWorkflowParameters;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

public final class GenericWorkflowClientExternalImpl implements GenericWorkflowClientExternal {

  private final String domain;
  private final IWorkflowService service;
  private final Scope metricsScope;

  public GenericWorkflowClientExternalImpl(
      IWorkflowService service, String domain, Scope metricsScope) {
    this.service = service;
    this.domain = domain;
    this.metricsScope = metricsScope;
  }

  @Override
  public String getDomain() {
    return domain;
  }

  @Override
  public IWorkflowService getService() {
    return service;
  }

  @Override
  public WorkflowExecution startWorkflow(StartWorkflowExecutionParameters startParameters)
      throws WorkflowExecutionAlreadyStartedError {
    try {
      return startWorkflowInternal(startParameters);
    } finally {
      emitMetricsForStartWorkflow(MetricsType.WORKFLOW_START_COUNTER, startParameters);
    }
  }

  @Override
  public CompletableFuture<WorkflowExecution> startWorkflowAsync(
      StartWorkflowExecutionParameters startParameters) {

    return startWorkflowAsync(startParameters, Long.MAX_VALUE);
  }

  @Override
  public CompletableFuture<WorkflowExecution> startWorkflowAsync(
      StartWorkflowExecutionParameters startParameters, Long timeoutInMillis) {

    emitMetricsForStartWorkflow(MetricsType.WORKFLOW_START_COUNTER, startParameters);
    return startWorkflowAsyncInternal(startParameters, timeoutInMillis);
  }

  private void emitMetricsForStartWorkflow(
      String type, StartWorkflowExecutionParameters startParameters) {
    // TODO: can probably cache this
    Map<String, String> tags =
        new ImmutableMap.Builder<String, String>(3)
            .put(MetricsTag.WORKFLOW_TYPE, startParameters.getWorkflowType().getName())
            .put(MetricsTag.TASK_LIST, startParameters.getTaskList())
            .put(MetricsTag.DOMAIN, domain)
            .build();
    metricsScope.tagged(tags).counter(type).inc(1);
  }

  private WorkflowExecution startWorkflowInternal(StartWorkflowExecutionParameters startParameters)
      throws WorkflowExecutionAlreadyStartedError {

    StartWorkflowExecutionRequest request = getStartRequest(startParameters);
    StartWorkflowExecutionResponse result;
    try {
      result =
          RpcRetryer.retryWithResult(
              RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, () -> service.StartWorkflowExecution(request));
    } catch (WorkflowExecutionAlreadyStartedError e) {
      throw e;
    } catch (TException e) {
      throw CheckedExceptionWrapper.wrap(e);
    }
    WorkflowExecution execution = new WorkflowExecution();
    execution.setRunId(result.getRunId());
    execution.setWorkflowId(request.getWorkflowId());

    return execution;
  }

  private RetryOptions getRetryOptionsWithExpiration(RetryOptions o, Long timeoutInMillis) {
    if (timeoutInMillis == null || timeoutInMillis <= 0 || timeoutInMillis == Long.MAX_VALUE) {
      return o;
    }
    return new RetryOptions.Builder(RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS)
        .setExpiration(Duration.ofMillis((timeoutInMillis)))
        .build();
  }

  private CompletableFuture<WorkflowExecution> startWorkflowAsyncInternal(
      StartWorkflowExecutionParameters startParameters, Long timeoutInMillis) {
    StartWorkflowExecutionRequest request = getStartRequest(startParameters);

    return RpcRetryer.retryWithResultAsync(
        getRetryOptionsWithExpiration(RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, timeoutInMillis),
        () -> {
          CompletableFuture<WorkflowExecution> result = new CompletableFuture<>();
          try {

            service.StartWorkflowExecutionWithTimeout(
                request,
                new AsyncMethodCallback<StartWorkflowExecutionResponse>() {
                  @Override
                  public void onComplete(StartWorkflowExecutionResponse response) {
                    WorkflowExecution execution = new WorkflowExecution();
                    execution.setRunId(response.getRunId());
                    execution.setWorkflowId(request.getWorkflowId());
                    result.complete(execution);
                  }

                  @Override
                  public void onError(Exception exception) {
                    result.completeExceptionally(exception);
                  }
                },
                timeoutInMillis);
          } catch (TException e) {
            result.completeExceptionally(e);
          }
          return result;
        });
  }

  @Override
  public void enqueueStartWorkflow(StartWorkflowExecutionParameters startParameters)
      throws WorkflowExecutionAlreadyStartedError {
    try {
      enqueueWorkflowInternal(startParameters);
    } finally {
      emitMetricsForStartWorkflow(MetricsType.WORKFLOW_START_ASYNC_COUNTER, startParameters);
    }
  }

  @Override
  public CompletableFuture<Void> enqueueStartWorkflowAsync(
      StartWorkflowExecutionParameters startParameters, Long timeoutInMillis) {
    emitMetricsForStartWorkflow(MetricsType.WORKFLOW_START_ASYNC_COUNTER, startParameters);
    return enqueueWorkflowAsyncInternal(startParameters, timeoutInMillis);
  }

  private void enqueueWorkflowInternal(StartWorkflowExecutionParameters startParameters)
      throws WorkflowExecutionAlreadyStartedError {
    StartWorkflowExecutionAsyncRequest request =
        new StartWorkflowExecutionAsyncRequest().setRequest(getStartRequest(startParameters));
    try {
      RpcRetryer.retryWithResult(
          RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, () -> service.StartWorkflowExecutionAsync(request));
    } catch (WorkflowExecutionAlreadyStartedError e) {
      throw e;
    } catch (TException e) {
      throw CheckedExceptionWrapper.wrap(e);
    }
  }

  private CompletableFuture<Void> enqueueWorkflowAsyncInternal(
      StartWorkflowExecutionParameters startParameters, Long timeoutInMillis) {
    StartWorkflowExecutionAsyncRequest request =
        new StartWorkflowExecutionAsyncRequest().setRequest(getStartRequest(startParameters));

    return RpcRetryer.retryWithResultAsync(
        getRetryOptionsWithExpiration(RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, timeoutInMillis),
        () -> {
          CompletableFuture<Void> result = new CompletableFuture<>();
          try {

            service.StartWorkflowExecutionAsyncWithTimeout(
                request,
                new AsyncMethodCallback<StartWorkflowExecutionAsyncResponse>() {
                  @Override
                  public void onComplete(StartWorkflowExecutionAsyncResponse response) {
                    result.complete(null);
                  }

                  @Override
                  public void onError(Exception exception) {
                    result.completeExceptionally(exception);
                  }
                },
                timeoutInMillis);
          } catch (TException e) {
            result.completeExceptionally(e);
          }
          return result;
        });
  }

  private StartWorkflowExecutionRequest getStartRequest(
      StartWorkflowExecutionParameters startParameters) {
    StartWorkflowExecutionRequest request =
        new StartWorkflowExecutionRequest().setRequestId(generateUniqueId());
    request.setDomain(domain);
    if (startParameters.getInput() != null) {
      request.setInput(startParameters.getInput());
    }
    request.setExecutionStartToCloseTimeoutSeconds(
        (int) startParameters.getExecutionStartToCloseTimeoutSeconds());
    request.setTaskStartToCloseTimeoutSeconds(
        (int) startParameters.getTaskStartToCloseTimeoutSeconds());
    request.setWorkflowIdReusePolicy(startParameters.getWorkflowIdReusePolicy());
    String taskList = startParameters.getTaskList();
    if (taskList != null && !taskList.isEmpty()) {
      TaskList tl = new TaskList();
      tl.setName(taskList);
      request.setTaskList(tl);
    }
    String workflowId = startParameters.getWorkflowId();
    if (workflowId == null) {
      workflowId = UUID.randomUUID().toString();
    }
    request.setWorkflowId(workflowId);
    request.setWorkflowType(startParameters.getWorkflowType());
    RetryParameters retryParameters = startParameters.getRetryParameters();
    if (retryParameters != null) {
      RetryPolicy retryPolicy = toRetryPolicy(retryParameters);
      request.setRetryPolicy(retryPolicy);
    }
    if (!Strings.isNullOrEmpty(startParameters.getCronSchedule())) {
      request.setCronSchedule(startParameters.getCronSchedule());
    }
    // cronOverlapPolicy not supported in thrift request yet
    request.setMemo(toMemoThrift(startParameters.getMemo()));
    request.setSearchAttributes(toSearchAttributesThrift(startParameters.getSearchAttributes()));
    request.setHeader(toHeaderThrift(startParameters.getContext()));
    if (startParameters.getDelayStart() != null) {
      request.setDelayStartSeconds((int) startParameters.getDelayStart().getSeconds());
    }

    return request;
  }

  private Memo toMemoThrift(Map<String, byte[]> memo) {
    if (memo == null || memo.isEmpty()) {
      return null;
    }

    Map<String, ByteBuffer> fields = new HashMap<>();
    for (Map.Entry<String, byte[]> item : memo.entrySet()) {
      fields.put(item.getKey(), ByteBuffer.wrap(item.getValue()));
    }
    Memo memoThrift = new Memo();
    memoThrift.setFields(fields);
    return memoThrift;
  }

  private SearchAttributes toSearchAttributesThrift(Map<String, byte[]> searchAttributes) {
    if (searchAttributes == null || searchAttributes.isEmpty()) {
      return null;
    }

    Map<String, ByteBuffer> fields = new HashMap<>();
    for (Map.Entry<String, byte[]> item : searchAttributes.entrySet()) {
      fields.put(item.getKey(), ByteBuffer.wrap(item.getValue()));
    }
    SearchAttributes searchAttrThrift = new SearchAttributes();
    searchAttrThrift.setIndexedFields(fields);
    return searchAttrThrift;
  }

  private Header toHeaderThrift(Map<String, byte[]> headers) {
    if (headers == null || headers.isEmpty()) {
      return null;
    }
    Map<String, ByteBuffer> fields = new HashMap<>();
    for (Map.Entry<String, byte[]> item : headers.entrySet()) {
      fields.put(item.getKey(), ByteBuffer.wrap(item.getValue()));
    }
    Header headerThrift = new Header();
    headerThrift.setFields(fields);
    return headerThrift;
  }

  private RetryPolicy toRetryPolicy(RetryParameters retryParameters) {
    return new RetryPolicy()
        .setBackoffCoefficient(retryParameters.getBackoffCoefficient())
        .setExpirationIntervalInSeconds(retryParameters.getExpirationIntervalInSeconds())
        .setInitialIntervalInSeconds(retryParameters.getInitialIntervalInSeconds())
        .setMaximumAttempts(retryParameters.getMaximumAttempts())
        .setMaximumIntervalInSeconds(retryParameters.getMaximumIntervalInSeconds())
        .setNonRetriableErrorReasons(retryParameters.getNonRetriableErrorReasons());
  }

  @Override
  public void signalWorkflowExecution(SignalExternalWorkflowParameters signalParameters) {
    SignalWorkflowExecutionRequest request = getSignalRequest(signalParameters);

    try {
      RpcRetryer.retry(() -> service.SignalWorkflowExecution(request));
    } catch (TException e) {
      throw CheckedExceptionWrapper.wrap(e);
    }
  }

  @Override
  public CompletableFuture<Void> signalWorkflowExecutionAsync(
      SignalExternalWorkflowParameters signalParameters) {
    return signalWorkflowExecutionAsync(signalParameters, Long.MAX_VALUE);
  }

  @Override
  public CompletableFuture<Void> signalWorkflowExecutionAsync(
      SignalExternalWorkflowParameters signalParameters, Long timeoutInMillis) {
    SignalWorkflowExecutionRequest request = getSignalRequest(signalParameters);
    return RpcRetryer.retryWithResultAsync(
        getRetryOptionsWithExpiration(RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, timeoutInMillis),
        () -> {
          CompletableFuture<Void> result = new CompletableFuture<>();
          try {
            service.SignalWorkflowExecution(
                request,
                new AsyncMethodCallback() {
                  @Override
                  public void onComplete(Object response) {
                    result.complete(null);
                  }

                  @Override
                  public void onError(Exception exception) {
                    result.completeExceptionally(exception);
                  }
                });
          } catch (TException e) {
            result.completeExceptionally(e);
          }
          return result;
        });
  }

  private SignalWorkflowExecutionRequest getSignalRequest(
      SignalExternalWorkflowParameters signalParameters) {
    SignalWorkflowExecutionRequest request =
        new SignalWorkflowExecutionRequest().setRequestId(generateUniqueId());
    request.setDomain(domain);
    request.setInput(signalParameters.getInput());
    request.setSignalName(signalParameters.getSignalName());
    WorkflowExecution execution = new WorkflowExecution();
    execution.setRunId(signalParameters.getRunId());
    execution.setWorkflowId(signalParameters.getWorkflowId());
    request.setWorkflowExecution(execution);
    return request;
  }

  @Override
  public WorkflowExecution signalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionParameters parameters) {
    try {
      return signalWithStartWorkflowInternal(parameters);
    } finally {
      Map<String, String> tags =
          new ImmutableMap.Builder<String, String>(3)
              .put(
                  MetricsTag.WORKFLOW_TYPE,
                  parameters.getStartParameters().getWorkflowType().getName())
              .put(MetricsTag.TASK_LIST, parameters.getStartParameters().getTaskList())
              .put(MetricsTag.DOMAIN, domain)
              .build();
      metricsScope.tagged(tags).counter(MetricsType.WORKFLOW_SIGNAL_WITH_START_COUNTER).inc(1);
    }
  }

  @Override
  public WorkflowExecution enqueueSignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionParameters parameters) {
    try {
      return enqueueSignalWithStartWorkflowInternal(parameters);
    } finally {
      Map<String, String> tags =
          new ImmutableMap.Builder<String, String>(3)
              .put(
                  MetricsTag.WORKFLOW_TYPE,
                  parameters.getStartParameters().getWorkflowType().getName())
              .put(MetricsTag.TASK_LIST, parameters.getStartParameters().getTaskList())
              .put(MetricsTag.DOMAIN, domain)
              .build();
      metricsScope
          .tagged(tags)
          .counter(MetricsType.WORKFLOW_SIGNAL_WITH_START_ASYNC_COUNTER)
          .inc(1);
    }
  }

  private WorkflowExecution enqueueSignalWithStartWorkflowInternal(
      SignalWithStartWorkflowExecutionParameters parameters) {
    SignalWithStartWorkflowExecutionAsyncRequest request =
        new SignalWithStartWorkflowExecutionAsyncRequest()
            .setRequest(createSignalWithStartRequest(parameters));
    try {
      RpcRetryer.retryWithResult(
          RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS,
          () -> service.SignalWithStartWorkflowExecutionAsync(request));
      return new WorkflowExecution().setWorkflowId(request.getRequest().getWorkflowId());
    } catch (TException e) {
      throw CheckedExceptionWrapper.wrap(e);
    }
  }

  private WorkflowExecution signalWithStartWorkflowInternal(
      SignalWithStartWorkflowExecutionParameters parameters) {
    SignalWithStartWorkflowExecutionRequest request = createSignalWithStartRequest(parameters);
    try {
      StartWorkflowExecutionResponse result =
          RpcRetryer.retryWithResult(
              RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS,
              () -> service.SignalWithStartWorkflowExecution(request));
      return new WorkflowExecution()
          .setRunId(result.getRunId())
          .setWorkflowId(request.getWorkflowId());
    } catch (TException e) {
      throw CheckedExceptionWrapper.wrap(e);
    }
  }

  private SignalWithStartWorkflowExecutionRequest createSignalWithStartRequest(
      SignalWithStartWorkflowExecutionParameters parameters) {
    SignalWithStartWorkflowExecutionRequest request =
        new SignalWithStartWorkflowExecutionRequest().setRequestId(generateUniqueId());
    request.setDomain(domain);
    StartWorkflowExecutionParameters startParameters = parameters.getStartParameters();
    request.setSignalName(parameters.getSignalName());
    request.setSignalInput(parameters.getSignalInput());
    // TODO        request.setIdentity()

    if (startParameters.getInput() != null) {
      request.setInput(startParameters.getInput());
    }
    request.setExecutionStartToCloseTimeoutSeconds(
        (int) startParameters.getExecutionStartToCloseTimeoutSeconds());
    request.setTaskStartToCloseTimeoutSeconds(
        (int) startParameters.getTaskStartToCloseTimeoutSeconds());
    request.setWorkflowIdReusePolicy(startParameters.getWorkflowIdReusePolicy());
    String taskList = startParameters.getTaskList();
    if (taskList != null && !taskList.isEmpty()) {
      TaskList tl = new TaskList();
      tl.setName(taskList);
      request.setTaskList(tl);
    }
    String workflowId = startParameters.getWorkflowId();
    if (workflowId == null) {
      workflowId = UUID.randomUUID().toString();
    }
    request.setWorkflowId(workflowId);
    request.setWorkflowType(startParameters.getWorkflowType());
    RetryParameters retryParameters = startParameters.getRetryParameters();
    if (retryParameters != null) {
      RetryPolicy retryPolicy = toRetryPolicy(retryParameters);
      request.setRetryPolicy(retryPolicy);
    }
    if (!Strings.isNullOrEmpty(startParameters.getCronSchedule())) {
      request.setCronSchedule(startParameters.getCronSchedule());
    }
    // cronOverlapPolicy not supported in thrift request yet
    if (startParameters.getDelayStart() != null) {
      request.setDelayStartSeconds((int) startParameters.getDelayStart().getSeconds());
    }
    return request;
  }

  @Override
  public void requestCancelWorkflowExecution(WorkflowExecution execution) {
    RequestCancelWorkflowExecutionRequest request =
        new RequestCancelWorkflowExecutionRequest().setRequestId(generateUniqueId());
    request.setDomain(domain);
    request.setWorkflowExecution(execution);
    try {
      RpcRetryer.retry(() -> service.RequestCancelWorkflowExecution(request));
    } catch (TException e) {
      throw CheckedExceptionWrapper.wrap(e);
    }
  }

  @Override
  public QueryWorkflowResponse queryWorkflow(QueryWorkflowParameters queryParameters) {
    QueryWorkflowRequest request = new QueryWorkflowRequest();
    request.setDomain(domain);
    WorkflowExecution execution = new WorkflowExecution();
    execution.setWorkflowId(queryParameters.getWorkflowId()).setRunId(queryParameters.getRunId());
    request.setExecution(execution);
    WorkflowQuery query = new WorkflowQuery();
    query.setQueryArgs(queryParameters.getInput());
    query.setQueryType(queryParameters.getQueryType());
    request.setQuery(query);
    request.setQueryRejectCondition(queryParameters.getQueryRejectCondition());
    request.setQueryConsistencyLevel(queryParameters.getQueryConsistencyLevel());
    try {
      QueryWorkflowResponse response =
          RpcRetryer.retryWithResult(
              RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, () -> service.QueryWorkflow(request));
      return response;
    } catch (TException e) {
      throw CheckedExceptionWrapper.wrap(e);
    }
  }

  @Override
  public String generateUniqueId() {
    return UUID.randomUUID().toString();
  }

  @Override
  public void terminateWorkflowExecution(TerminateWorkflowExecutionParameters terminateParameters) {
    TerminateWorkflowExecutionRequest request = new TerminateWorkflowExecutionRequest();
    request.setWorkflowExecution(terminateParameters.getWorkflowExecution());
    request.setDomain(domain);
    request.setDetails(terminateParameters.getDetails());
    request.setReason(terminateParameters.getReason());
    //        request.setChildPolicy(terminateParameters.getChildPolicy());
    try {
      RpcRetryer.retry(() -> service.TerminateWorkflowExecution(request));
    } catch (TException e) {
      throw CheckedExceptionWrapper.wrap(e);
    }
  }
}
