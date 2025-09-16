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
package com.uber.cadence.internal.worker;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.uber.cadence.CadenceError;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.NoopScope;
import org.junit.Before;
import org.junit.Test;

public class LocallyDispatchedActivityPollTaskTest {

  private WorkflowClient mockClient;
  private IWorkflowService mockService;
  private LocallyDispatchedActivityPollTask pollTask;
  private LocallyDispatchedActivityWorker.Task mockTask;
  private SingleWorkerOptions options;

  @Before
  public void setup() throws Exception {
    mockClient = mock(WorkflowClient.class);
    mockService = mock(IWorkflowService.class);

    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder().setMetricsScope(new NoopScope()).build();
    when(mockClient.getOptions()).thenReturn(clientOptions);
    when(mockClient.getService()).thenReturn(mockService);
    when(mockService.getOptions()).thenReturn(ClientOptions.defaultInstance());

    options =
        SingleWorkerOptions.newBuilder().setMetricsScope(clientOptions.getMetricsScope()).build();
    pollTask = new LocallyDispatchedActivityPollTask(options);
    mockTask = mock(LocallyDispatchedActivityWorker.Task.class);
  }

  @Test
  public void testPollTaskInterruptedException() throws Exception {
    Thread.currentThread().interrupt();
    pollTask.apply(mockTask);
    try {
      pollTask.pollTask();
      fail("Expected RuntimeException due to interruption");
    } catch (CadenceError e) {
      fail("Unexpected BaseError");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("locally dispatch activity poll task interrupted"));
    } finally {
      Thread.interrupted();
    }
  }
}
