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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uber.cadence.ActiveClusterSelectionPolicy;
import com.uber.cadence.ClusterAttribute;
import com.uber.cadence.CronOverlapPolicy;
import com.uber.cadence.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionResponse;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.internal.common.SignalWithStartWorkflowExecutionParameters;
import com.uber.cadence.internal.common.StartWorkflowExecutionParameters;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.NoopScope;
import java.time.Duration;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GenericWorkflowClientExternalImplTest {

  private static final ActiveClusterSelectionPolicy POLICY =
      new ActiveClusterSelectionPolicy()
          .setClusterAttribute(new ClusterAttribute().setScope("location").setName("lisbon"));

  private IWorkflowService service;
  private GenericWorkflowClientExternalImpl client;

  @Before
  public void setUp() {
    service = mock(IWorkflowService.class);
    client = new GenericWorkflowClientExternalImpl(service, "test-domain", new NoopScope());
  }

  private StartWorkflowExecutionParameters startParameters() {
    StartWorkflowExecutionParameters parameters = new StartWorkflowExecutionParameters();
    parameters.setWorkflowId("wid");
    parameters.setWorkflowType(new WorkflowType().setName("wt"));
    parameters.setTaskList("tl");
    parameters.setExecutionStartToCloseTimeoutSeconds(10);
    parameters.setTaskStartToCloseTimeoutSeconds(10);
    parameters.setActiveClusterSelectionPolicy(POLICY);
    return parameters;
  }

  @Test
  public void startWorkflowCarriesActiveClusterSelectionPolicy() throws Exception {
    when(service.StartWorkflowExecution(any()))
        .thenReturn(new StartWorkflowExecutionResponse().setRunId("rid"));

    client.startWorkflow(startParameters());

    ArgumentCaptor<StartWorkflowExecutionRequest> captor =
        ArgumentCaptor.forClass(StartWorkflowExecutionRequest.class);
    verify(service).StartWorkflowExecution(captor.capture());
    assertEquals(POLICY, captor.getValue().getActiveClusterSelectionPolicy());
  }

  @Test
  public void signalWithStartCarriesActiveClusterSelectionPolicy() throws Exception {
    when(service.SignalWithStartWorkflowExecution(any()))
        .thenReturn(new com.uber.cadence.StartWorkflowExecutionResponse().setRunId("rid"));

    SignalWithStartWorkflowExecutionParameters parameters =
        new SignalWithStartWorkflowExecutionParameters(startParameters(), "signal", new byte[] {1});
    client.signalWithStartWorkflowExecution(parameters);

    ArgumentCaptor<SignalWithStartWorkflowExecutionRequest> captor =
        ArgumentCaptor.forClass(SignalWithStartWorkflowExecutionRequest.class);
    verify(service).SignalWithStartWorkflowExecution(captor.capture());
    assertEquals(POLICY, captor.getValue().getActiveClusterSelectionPolicy());
  }

  // 2030-01-02T03:04:05.123456789Z in Unix nanoseconds.
  private static final Instant FIRST_RUN_AT = Instant.parse("2030-01-02T03:04:05.123456789Z");
  private static final long FIRST_RUN_AT_NANOS = 1893553445123456789L;

  private StartWorkflowExecutionParameters startParametersWithStartTiming() {
    StartWorkflowExecutionParameters parameters = startParameters();
    parameters.setJitterStart(Duration.ofSeconds(30));
    parameters.setFirstRunAt(FIRST_RUN_AT);
    parameters.setCronOverlapPolicy(CronOverlapPolicy.BUFFERONE);
    return parameters;
  }

  @Test
  public void startWorkflowCarriesStartTimingOptions() throws Exception {
    when(service.StartWorkflowExecution(any()))
        .thenReturn(new StartWorkflowExecutionResponse().setRunId("rid"));

    client.startWorkflow(startParametersWithStartTiming());

    ArgumentCaptor<StartWorkflowExecutionRequest> captor =
        ArgumentCaptor.forClass(StartWorkflowExecutionRequest.class);
    verify(service).StartWorkflowExecution(captor.capture());
    StartWorkflowExecutionRequest request = captor.getValue();
    assertEquals(30, request.getJitterStartSeconds());
    assertEquals(FIRST_RUN_AT_NANOS, request.getFirstRunAtTimestamp());
    assertEquals(CronOverlapPolicy.BUFFERONE, request.getCronOverlapPolicy());
  }

  @Test
  public void startWorkflowLeavesStartTimingOptionsUnsetByDefault() throws Exception {
    when(service.StartWorkflowExecution(any()))
        .thenReturn(new StartWorkflowExecutionResponse().setRunId("rid"));

    client.startWorkflow(startParameters());

    ArgumentCaptor<StartWorkflowExecutionRequest> captor =
        ArgumentCaptor.forClass(StartWorkflowExecutionRequest.class);
    verify(service).StartWorkflowExecution(captor.capture());
    StartWorkflowExecutionRequest request = captor.getValue();
    assertEquals(0, request.getJitterStartSeconds());
    assertEquals(0, request.getFirstRunAtTimestamp());
    assertNull(request.getCronOverlapPolicy());
  }

  @Test
  public void signalWithStartCarriesStartTimingOptions() throws Exception {
    when(service.SignalWithStartWorkflowExecution(any()))
        .thenReturn(new com.uber.cadence.StartWorkflowExecutionResponse().setRunId("rid"));

    SignalWithStartWorkflowExecutionParameters parameters =
        new SignalWithStartWorkflowExecutionParameters(
            startParametersWithStartTiming(), "signal", new byte[] {1});
    client.signalWithStartWorkflowExecution(parameters);

    ArgumentCaptor<SignalWithStartWorkflowExecutionRequest> captor =
        ArgumentCaptor.forClass(SignalWithStartWorkflowExecutionRequest.class);
    verify(service).SignalWithStartWorkflowExecution(captor.capture());
    SignalWithStartWorkflowExecutionRequest request = captor.getValue();
    assertEquals(30, request.getJitterStartSeconds());
    assertEquals(FIRST_RUN_AT_NANOS, request.getFirstRunAtTimestamp());
    assertEquals(CronOverlapPolicy.BUFFERONE, request.getCronOverlapPolicy());
  }
}
