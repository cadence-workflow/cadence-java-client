/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.uber.cadence.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.TaskList;
import com.uber.cadence.WorkflowIdReusePolicy;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.shadower.Constants;
import com.uber.cadence.shadower.ExitCondition;
import com.uber.cadence.shadower.Mode;
import com.uber.cadence.shadower.WorkflowParams;
import com.uber.m3.tally.NoopScope;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

public class ShadowingWorkerTest {
  private WorkflowClient mockClient = mock(WorkflowClient.class);
  private IWorkflowService mockService = mock(IWorkflowService.class);

  @Before
  public void init() {
    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder().setMetricsScope(new NoopScope()).build();
    when(mockClient.getOptions()).thenReturn(clientOptions);
    when(mockClient.getService()).thenReturn(mockService);
  }

  @Test
  public void testStartShadowingWorkflow_ReceiveExpectedRequest() throws Exception {
    String taskList = UUID.randomUUID().toString();
    ShadowingOptions shadowingOptions =
        ShadowingOptions.newBuilder()
            .setDomain(UUID.randomUUID().toString())
            .setWorkflowTypes(Lists.newArrayList("type1", "type2"))
            .setConcurrency(2)
            .setShadowMode(Mode.Continuous)
            .setWorkflowSamplingRate(0.1)
            .setWorkflowStatuses(Lists.newArrayList(WorkflowStatus.CLOSED))
            .setExitCondition(new ExitCondition().setShadowCount(10))
            .setWorkflowStartTimeFilter(
                TimeFilter.newBuilder().setMinTimestamp(ZonedDateTime.now()).build())
            .build();
    ShadowingWorker shadowingWorker =
        new ShadowingWorker(
            mockClient, taskList, WorkerOptions.defaultInstance(), shadowingOptions);

    WorkflowParams params =
        new WorkflowParams()
            .setDomain(shadowingOptions.getDomain())
            .setConcurrency(shadowingOptions.getConcurrency())
            .setExitCondition(shadowingOptions.getExitCondition())
            .setShadowMode(shadowingOptions.getShadowMode())
            .setSamplingRate(shadowingOptions.getSamplingRate())
            .setTaskList(shadowingOptions.getDomain() + "-" + taskList)
            .setWorkflowQuery(shadowingOptions.getWorkflowQuery());
    StartWorkflowExecutionRequest expectedRequest =
        new StartWorkflowExecutionRequest()
            .setDomain(Constants.LocalDomainName)
            .setWorkflowId(shadowingOptions.getDomain() + Constants.WorkflowIDSuffix)
            .setTaskList(new TaskList().setName(Constants.TaskList))
            .setWorkflowType(new WorkflowType().setName(Constants.WorkflowName))
            .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.AllowDuplicate)
            .setExecutionStartToCloseTimeoutSeconds(864000)
            .setTaskStartToCloseTimeoutSeconds(60)
            .setInput(JsonDataConverter.getInstance().toData(params));
    when(mockService.StartWorkflowExecution(any())).thenReturn(null);

    shadowingWorker.startShadowingWorkflow();
    verify(mockService)
        .StartWorkflowExecution(argThat(new StartWorkflowExecutionRequestMatcher(expectedRequest)));
  }

  private class StartWorkflowExecutionRequestMatcher
      implements ArgumentMatcher<StartWorkflowExecutionRequest> {
    StartWorkflowExecutionRequest request;

    public StartWorkflowExecutionRequestMatcher(StartWorkflowExecutionRequest request) {
      this.request = request;
    }

    @Override
    public boolean matches(StartWorkflowExecutionRequest argument) {
      StartWorkflowExecutionRequest newRequest = (StartWorkflowExecutionRequest) argument;
      return request.getDomain().equals(newRequest.getDomain())
          && request.getTaskList().equals(newRequest.getTaskList())
          && request.getWorkflowId().equals(newRequest.getWorkflowId())
          && request.getWorkflowIdReusePolicy().equals(newRequest.getWorkflowIdReusePolicy())
          && request.getWorkflowType().equals(newRequest.getWorkflowType())
          && Arrays.equals(request.getInput(), newRequest.getInput())
          && request.getExecutionStartToCloseTimeoutSeconds()
              == newRequest.getExecutionStartToCloseTimeoutSeconds()
          && request.getTaskStartToCloseTimeoutSeconds()
              == newRequest.getTaskStartToCloseTimeoutSeconds();
    }
  }
}
