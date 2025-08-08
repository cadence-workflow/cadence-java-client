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

import static com.uber.cadence.internal.metrics.MetricsTagValue.INTERNAL_SERVICE_ERROR;
import static com.uber.cadence.internal.metrics.MetricsTagValue.SERVICE_BUSY;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import com.uber.cadence.*;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.Counter;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.Stopwatch;
import com.uber.m3.tally.Timer;
import org.junit.Before;
import org.junit.Test;

public class ActivityPollTaskTest {

  private IWorkflowService mockService;
  private SingleWorkerOptions options;
  private ActivityPollTask pollTask;

  @Before
  public void setup() {
    mockService = mock(IWorkflowService.class);
    Scope metricsScope = mock(Scope.class); // Mock Scope to avoid NoopScope

    // Mock the Timer and Stopwatch
    Timer timer = mock(Timer.class);
    Stopwatch stopwatch = mock(Stopwatch.class);
    when(metricsScope.timer(MetricsType.ACTIVITY_POLL_LATENCY)).thenReturn(timer);
    when(timer.start()).thenReturn(stopwatch);

    // Mock the Counter and its inc() method
    Counter counter = mock(Counter.class);
    when(metricsScope.counter(MetricsType.ACTIVITY_POLL_COUNTER)).thenReturn(counter);
    when(metricsScope.counter(MetricsType.ACTIVITY_POLL_NO_TASK_COUNTER)).thenReturn(counter);
    when(metricsScope.counter(MetricsType.ACTIVITY_POLL_FAILED_COUNTER)).thenReturn(counter);
    when(metricsScope.counter(MetricsType.ACTIVITY_POLL_TRANSIENT_FAILED_COUNTER))
        .thenReturn(counter);

    // Use doNothing() to stub the inc() method of Counter, as it returns void
    doNothing().when(counter).inc(anyInt());

    // Set up SingleWorkerOptions to return the mocked metricsScope
    options = mock(SingleWorkerOptions.class);
    when(options.getMetricsScope())
        .thenReturn(metricsScope); // Set options to return the mock Scope
    when(options.getIdentity()).thenReturn("test-identity");
    when(options.getTaskListActivitiesPerSecond()).thenReturn(1.0);

    // Initialize pollTask with mocked options
    pollTask = new ActivityPollTask(mockService, "test-domain", "test-taskList", options);
  }

  @Test
  public void testPollTaskSuccess() throws BaseError {
    PollForActivityTaskResponse response =
        new PollForActivityTaskResponse().setTaskToken("testToken".getBytes());
    when(mockService.PollForActivityTask(any(PollForActivityTaskRequest.class)))
        .thenReturn(response);

    // Mock the timer and stopwatch behavior
    Scope metricsScope = options.getMetricsScope();
    Timer timer = mock(Timer.class);
    when(metricsScope.timer(MetricsType.ACTIVITY_POLL_LATENCY)).thenReturn(timer);
    Stopwatch sw = mock(Stopwatch.class);
    when(timer.start()).thenReturn(sw);

    PollForActivityTaskResponse result = pollTask.pollTask();

    assertNotNull(result);
    assertArrayEquals("testToken".getBytes(), result.getTaskToken());

    // Verify the counters and the timer behavior
    verify(metricsScope.counter(MetricsType.ACTIVITY_POLL_COUNTER), times(1)).inc(1);
    verify(timer, times(1)).start();
    verify(sw, times(1)).stop();
  }

  @Test(expected = InternalServiceError.class)
  public void testPollTaskInternalServiceError() throws BaseError {
    // Set up mockService to throw an InternalServiceError exception
    when(mockService.PollForActivityTask(any(PollForActivityTaskRequest.class)))
        .thenThrow(new InternalServiceError());

    // Mock taggedScope and taggedCounter to ensure the behavior
    Scope metricsScope = options.getMetricsScope();
    Scope taggedScope = mock(Scope.class);
    Counter taggedCounter = mock(Counter.class);

    // Set up taggedScope to return taggedCounter for specific counter calls
    when(metricsScope.tagged(ImmutableMap.of(MetricsTag.CAUSE, INTERNAL_SERVICE_ERROR)))
        .thenReturn(taggedScope);
    when(taggedScope.counter(MetricsType.ACTIVITY_POLL_TRANSIENT_FAILED_COUNTER))
        .thenReturn(taggedCounter);

    try {
      // Call pollTask.pollTask(), expecting an InternalServiceError to be thrown
      pollTask.pollTask();
    } finally {
      // Verify that taggedCounter.inc(1) is called once
      verify(taggedCounter, times(1)).inc(1);
    }
  }

  @Test(expected = ServiceBusyError.class)
  public void testPollTaskServiceBusyError() throws BaseError {
    // Set up mockService to throw a ServiceBusyError exception
    when(mockService.PollForActivityTask(any(PollForActivityTaskRequest.class)))
        .thenThrow(new ServiceBusyError());

    // Mock taggedScope and taggedCounter to ensure the behavior
    Scope metricsScope = options.getMetricsScope();
    Scope taggedScope = mock(Scope.class);
    Counter taggedCounter = mock(Counter.class);

    // Set up taggedScope to return taggedCounter for specific counter calls
    when(metricsScope.tagged(ImmutableMap.of(MetricsTag.CAUSE, SERVICE_BUSY)))
        .thenReturn(taggedScope);
    when(taggedScope.counter(MetricsType.ACTIVITY_POLL_TRANSIENT_FAILED_COUNTER))
        .thenReturn(taggedCounter);

    try {
      // Call pollTask.pollTask(), expecting a ServiceBusyError to be thrown
      pollTask.pollTask();
    } finally {
      // Verify that taggedCounter.inc(1) is called once
      verify(taggedCounter, times(1)).inc(1);
    }
  }

  @Test(expected = BaseError.class)
  public void testPollTaskGeneralTException() throws BaseError {
    // Set up mockService to throw a BaseError
    when(mockService.PollForActivityTask(any(PollForActivityTaskRequest.class)))
        .thenThrow(new BaseError());

    // Mock the metricsScope and counter to ensure proper behavior
    Scope metricsScope = options.getMetricsScope();
    Counter failedCounter = mock(Counter.class);
    when(metricsScope.counter(MetricsType.ACTIVITY_POLL_FAILED_COUNTER)).thenReturn(failedCounter);

    try {
      // Call pollTask.pollTask(), expecting a BaseError to be thrown
      pollTask.pollTask();
    } finally {
      // Verify that failedCounter.inc(1) is called once
      verify(failedCounter, times(1)).inc(1);
    }
  }

  @Test
  public void testPollTaskNoTask() throws BaseError {
    // Set up mockService to return an empty PollForActivityTaskResponse
    when(mockService.PollForActivityTask(any(PollForActivityTaskRequest.class)))
        .thenReturn(new PollForActivityTaskResponse());

    // Mock the metricsScope and noTaskCounter to ensure proper behavior
    Scope metricsScope = options.getMetricsScope();
    Counter noTaskCounter = mock(Counter.class);
    when(metricsScope.counter(MetricsType.ACTIVITY_POLL_NO_TASK_COUNTER)).thenReturn(noTaskCounter);

    // Call pollTask.pollTask() and check the response
    PollForActivityTaskResponse result = pollTask.pollTask();

    // Verify that the result is null when there is no task
    assertNull(result);

    // Verify that noTaskCounter.inc(1) is called once
    verify(noTaskCounter, times(1)).inc(1);
  }
}
