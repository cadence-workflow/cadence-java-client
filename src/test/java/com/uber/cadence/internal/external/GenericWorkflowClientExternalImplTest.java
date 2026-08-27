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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uber.cadence.ActiveClusterSelectionPolicy;
import com.uber.cadence.ClusterAttribute;
import com.uber.cadence.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionResponse;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.internal.common.SignalWithStartWorkflowExecutionParameters;
import com.uber.cadence.internal.common.StartWorkflowExecutionParameters;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.NoopScope;
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
}
