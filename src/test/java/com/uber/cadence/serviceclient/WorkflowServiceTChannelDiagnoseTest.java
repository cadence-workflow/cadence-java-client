/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.serviceclient;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import com.uber.cadence.DiagnoseWorkflowExecutionRequest;
import com.uber.tchannel.api.SubChannel;
import org.apache.thrift.async.AsyncMethodCallback;
import org.junit.Before;
import org.junit.Test;

public class WorkflowServiceTChannelDiagnoseTest {

  private WorkflowServiceTChannel service;

  @Before
  public void setUp() {
    SubChannel mockSubChannel = mock(SubChannel.class);
    service = new WorkflowServiceTChannel(mockSubChannel, ClientOptions.newBuilder().build());
  }

  @Test
  public void testDiagnoseWorkflowExecutionThrowsUnsupportedOperation() {
    DiagnoseWorkflowExecutionRequest request = new DiagnoseWorkflowExecutionRequest();
    assertThrows(
        UnsupportedOperationException.class, () -> service.DiagnoseWorkflowExecution(request));
  }

  @Test
  public void testDiagnoseWorkflowExecutionAsyncThrowsUnsupportedOperation() {
    DiagnoseWorkflowExecutionRequest request = new DiagnoseWorkflowExecutionRequest();
    AsyncMethodCallback callback =
        new AsyncMethodCallback() {
          @Override
          public void onComplete(Object response) {}

          @Override
          public void onError(Exception exception) {}
        };

    assertThrows(
        UnsupportedOperationException.class,
        () -> service.DiagnoseWorkflowExecution(request, callback));
  }
}
