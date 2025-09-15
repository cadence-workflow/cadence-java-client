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
import com.uber.m3.util.Duration;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

public class WorkflowPollTaskTest {

  private IWorkflowService mockService;
  private Scope mockMetricScope;
  private WorkflowPollTask pollTask;

  @Before
  public void setup() {
    mockService = mock(IWorkflowService.class);
    mockMetricScope = mock(Scope.class);

    // Mock the Timer and Stopwatch
    Timer pollLatencyTimer = mock(Timer.class);
    Timer scheduledToStartLatencyTimer = mock(Timer.class);
    Stopwatch sw = mock(Stopwatch.class);

    // Ensure timers and stopwatch are not null and return expected values
    when(mockMetricScope.timer(MetricsType.DECISION_POLL_LATENCY)).thenReturn(pollLatencyTimer);
    when(pollLatencyTimer.start()).thenReturn(sw);
    when(mockMetricScope.timer(MetricsType.DECISION_SCHEDULED_TO_START_LATENCY))
        .thenReturn(scheduledToStartLatencyTimer);
    doNothing().when(scheduledToStartLatencyTimer).record(any(Duration.class));

    // Mock counters for different metrics
    Counter pollCounter = mock(Counter.class);
    Counter succeedCounter = mock(Counter.class);
    Counter noTaskCounter = mock(Counter.class);
    Counter failedCounter = mock(Counter.class);
    Counter transientFailedCounter = mock(Counter.class);

    // Set up mockMetricScope to return these counters for specific metrics
    when(mockMetricScope.counter(MetricsType.DECISION_POLL_COUNTER)).thenReturn(pollCounter);
    when(mockMetricScope.counter(MetricsType.DECISION_POLL_NO_TASK_COUNTER))
        .thenReturn(noTaskCounter);
    when(mockMetricScope.counter(MetricsType.DECISION_POLL_FAILED_COUNTER))
        .thenReturn(failedCounter);
    when(mockMetricScope.counter(MetricsType.DECISION_POLL_TRANSIENT_FAILED_COUNTER))
        .thenReturn(transientFailedCounter);
    when(mockMetricScope.counter(MetricsType.DECISION_POLL_SUCCEED_COUNTER))
        .thenReturn(succeedCounter);

    // Initialize pollTask with the mocked dependencies
    pollTask =
        new WorkflowPollTask(
            mockService,
            "test-domain",
            "test-taskList",
            TaskListKind.NORMAL,
            mockMetricScope,
            "test-identity");
  }

  @Test
  public void testPollSuccess() throws TException {
    // Mock a successful response with all necessary fields
    WorkflowType workflowType = new WorkflowType().setName("testWorkflowType");

    PollForDecisionTaskResponse response =
        new PollForDecisionTaskResponse()
            .setTaskToken("testToken".getBytes())
            .setWorkflowType(workflowType)
            .setScheduledTimestamp(1000L) // Ensure ScheduledTimestamp is non-null
            .setStartedTimestamp(2000L); // Ensure StartedTimestamp is non-null

    when(mockService.PollForDecisionTask(any(PollForDecisionTaskRequest.class)))
        .thenReturn(response);

    // Mock the timer and stopwatch behavior
    Timer pollLatencyTimer = mock(Timer.class);
    Timer scheduledToStartLatencyTimer = mock(Timer.class);
    Stopwatch sw = mock(Stopwatch.class);
    when(mockMetricScope.timer(MetricsType.DECISION_POLL_LATENCY)).thenReturn(pollLatencyTimer);
    when(pollLatencyTimer.start()).thenReturn(sw);

    // Mock the tagged scope for workflow type
    Scope taggedScope = mock(Scope.class);
    when(mockMetricScope.tagged(ImmutableMap.of(MetricsTag.WORKFLOW_TYPE, "testWorkflowType")))
        .thenReturn(taggedScope);

    // Ensure DECISION_SCHEDULED_TO_START_LATENCY timer in taggedScope is not null
    when(taggedScope.timer(MetricsType.DECISION_SCHEDULED_TO_START_LATENCY))
        .thenReturn(scheduledToStartLatencyTimer);
    doNothing().when(scheduledToStartLatencyTimer).record(any(Duration.class));

    // Mock counters for DECISION_POLL_COUNTER and DECISION_POLL_SUCCEED_COUNTER
    Counter pollCounter = mock(Counter.class);
    Counter succeedCounter = mock(Counter.class);
    when(mockMetricScope.counter(MetricsType.DECISION_POLL_COUNTER)).thenReturn(pollCounter);
    when(taggedScope.counter(MetricsType.DECISION_POLL_SUCCEED_COUNTER)).thenReturn(succeedCounter);

    PollForDecisionTaskResponse result = pollTask.poll();

    // Verify that the result is not null and task token is as expected
    assertNotNull(result);
    assertArrayEquals("testToken".getBytes(), result.getTaskToken());

    // Verify counter and timer behavior
    verify(pollCounter, times(1)).inc(1);
    verify(succeedCounter, times(1)).inc(1);
    verify(pollLatencyTimer, times(1)).start();
    verify(sw, times(1)).stop();

    // Verify that record() on scheduledToStartLatencyTimer was called with correct duration
    Duration expectedDuration =
        Duration.ofNanos(result.getStartedTimestamp() - result.getScheduledTimestamp());
    verify(scheduledToStartLatencyTimer, times(1)).record(eq(expectedDuration));
  }

  @Test(expected = InternalServiceError.class)
  public void testPollInternalServiceError() throws TException {
    when(mockService.PollForDecisionTask(any(PollForDecisionTaskRequest.class)))
        .thenThrow(new InternalServiceError());

    Scope taggedScope = mock(Scope.class);
    Counter taggedCounter = mock(Counter.class);
    when(mockMetricScope.tagged(ImmutableMap.of(MetricsTag.CAUSE, INTERNAL_SERVICE_ERROR)))
        .thenReturn(taggedScope);
    when(taggedScope.counter(MetricsType.DECISION_POLL_TRANSIENT_FAILED_COUNTER))
        .thenReturn(taggedCounter);

    try {
      pollTask.poll();
    } finally {
      verify(taggedCounter, times(1)).inc(1);
    }
  }

  @Test(expected = ServiceBusyError.class)
  public void testPollServiceBusyError() throws TException {
    when(mockService.PollForDecisionTask(any(PollForDecisionTaskRequest.class)))
        .thenThrow(new ServiceBusyError());

    Scope taggedScope = mock(Scope.class);
    Counter taggedCounter = mock(Counter.class);
    when(mockMetricScope.tagged(ImmutableMap.of(MetricsTag.CAUSE, SERVICE_BUSY)))
        .thenReturn(taggedScope);
    when(taggedScope.counter(MetricsType.DECISION_POLL_TRANSIENT_FAILED_COUNTER))
        .thenReturn(taggedCounter);

    try {
      pollTask.poll();
    } finally {
      verify(taggedCounter, times(1)).inc(1);
    }
  }

  @Test(expected = TException.class)
  public void testPollGeneralTException() throws TException {
    when(mockService.PollForDecisionTask(any(PollForDecisionTaskRequest.class)))
        .thenThrow(new TException());

    Counter failedCounter = mock(Counter.class);
    when(mockMetricScope.counter(MetricsType.DECISION_POLL_FAILED_COUNTER))
        .thenReturn(failedCounter);

    try {
      pollTask.poll();
    } finally {
      verify(failedCounter, times(1)).inc(1);
    }
  }

  @Test
  public void testPollNoTask() throws TException {
    when(mockService.PollForDecisionTask(any(PollForDecisionTaskRequest.class)))
        .thenReturn(new PollForDecisionTaskResponse());

    Counter noTaskCounter = mock(Counter.class);
    when(mockMetricScope.counter(MetricsType.DECISION_POLL_NO_TASK_COUNTER))
        .thenReturn(noTaskCounter);

    PollForDecisionTaskResponse result = pollTask.poll();

    assertNull(result);
    verify(noTaskCounter, times(1)).inc(1);
  }
}
