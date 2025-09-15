/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
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
package com.uber.cadence.internal.shadowing;

import static com.uber.cadence.EventType.DecisionTaskCompleted;
import static com.uber.cadence.EventType.DecisionTaskScheduled;
import static com.uber.cadence.EventType.DecisionTaskStarted;
import static com.uber.cadence.EventType.TimerStarted;
import static com.uber.cadence.EventType.WorkflowExecutionStarted;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.uber.cadence.DataBlob;
import com.uber.cadence.DecisionTaskCompletedEventAttributes;
import com.uber.cadence.DecisionTaskScheduledEventAttributes;
import com.uber.cadence.DecisionTaskStartedEventAttributes;
import com.uber.cadence.GetWorkflowExecutionHistoryResponse;
import com.uber.cadence.History;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.TaskList;
import com.uber.cadence.TaskListKind;
import com.uber.cadence.TimerStartedEventAttributes;
import com.uber.cadence.WorkflowExecutionStartedEventAttributes;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.common.WorkflowExecutionHistory;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.internal.testing.WorkflowTestingTest;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.testing.TestActivityEnvironment;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class ReplayWorkflowActivityTest {

  private TestActivityEnvironment testEnv;
  private IWorkflowService mockServiceClient;
  private Scope metricsScope;
  private ReplayWorkflowActivityImpl activity;
  private ReplayWorkflowActivity activityStub;

  private String domain;
  private WorkflowExecution execution;
  private List<HistoryEvent> historyEvents;

  @Before
  public void init() {
    mockServiceClient = mock(IWorkflowService.class);
    metricsScope = new RootScopeBuilder().reportEvery(Duration.ofMillis(1000));
    TestEnvironmentOptions testOptions =
        new TestEnvironmentOptions.Builder()
            .setDataConverter(JsonDataConverter.getInstance())
            .build();
    activity = new ReplayWorkflowActivityImpl(mockServiceClient, metricsScope, "test", testOptions);
    activity.registerWorkflowImplementationTypes(WorkflowTestingTest.EmptyWorkflowImpl.class);

    domain = UUID.randomUUID().toString();
    execution = new WorkflowExecution("wid", "rid");
    reset();

    testEnv = TestActivityEnvironment.newInstance();
    testEnv.registerActivitiesImplementations(activity);
    activityStub = testEnv.newActivityStub(ReplayWorkflowActivity.class);
  }

  private void reset() {
    HistoryEvent event1 =
        new HistoryEvent()
            .setEventId(1)
            .setVersion(1)
            .setEventType(WorkflowExecutionStarted)
            .setTimestamp(ZonedDateTime.now().toEpochSecond())
            .setWorkflowExecutionStartedEventAttributes(
                new WorkflowExecutionStartedEventAttributes()
                    .setAttempt(1)
                    .setTaskList(new TaskList().setKind(TaskListKind.NORMAL).setName("tasklist"))
                    .setFirstExecutionRunId(execution.getRunId())
                    .setWorkflowType(new WorkflowType().setName("TestWorkflow::workflow1")));
    HistoryEvent event2 =
        new HistoryEvent()
            .setEventId(2)
            .setVersion(1)
            .setTimestamp(ZonedDateTime.now().toEpochSecond())
            .setEventType(DecisionTaskScheduled)
            .setDecisionTaskScheduledEventAttributes(new DecisionTaskScheduledEventAttributes());
    HistoryEvent event3 =
        new HistoryEvent()
            .setEventId(3)
            .setVersion(1)
            .setTimestamp(ZonedDateTime.now().toEpochSecond())
            .setEventType(DecisionTaskStarted)
            .setDecisionTaskStartedEventAttributes(new DecisionTaskStartedEventAttributes());
    HistoryEvent event4 =
        new HistoryEvent()
            .setEventId(4)
            .setVersion(1)
            .setTimestamp(ZonedDateTime.now().toEpochSecond())
            .setEventType(DecisionTaskCompleted)
            .setDecisionTaskCompletedEventAttributes(new DecisionTaskCompletedEventAttributes());
    HistoryEvent event5 =
        new HistoryEvent()
            .setEventId(5)
            .setVersion(1)
            .setTimestamp(ZonedDateTime.now().toEpochSecond())
            .setEventType(TimerStarted)
            .setTimerStartedEventAttributes(new TimerStartedEventAttributes());
    historyEvents = Lists.newArrayList(event1, event2, event3, event4, event5);
  }

  @Test
  public void testGetFullHistory_DecodedHistory_ExpectedSuccessResponse() throws Exception {
    History history = new History().setEvents(Lists.newArrayList(historyEvents.get(0)));
    GetWorkflowExecutionHistoryResponse response =
        new GetWorkflowExecutionHistoryResponse().setHistory(history);
    when(mockServiceClient.GetWorkflowExecutionHistory(any())).thenReturn(response);

    WorkflowExecutionHistory result = activity.getFullHistory(domain, execution);
    assertEquals(1, result.getEvents().size());
  }

  @Test
  public void testGetFullHistory_RawHistory_NotSupportedError() throws Exception {
    History history = new History().setEvents(Lists.newArrayList(historyEvents.get(0)));
    DataBlob blob = new DataBlob().setData(new byte[] {1, 2, 3});

    GetWorkflowExecutionHistoryResponse response =
        new GetWorkflowExecutionHistoryResponse().setRawHistory(Lists.newArrayList(blob));
    when(mockServiceClient.GetWorkflowExecutionHistory(any())).thenReturn(response);
    try {
      WorkflowExecutionHistory result = activity.getFullHistory(domain, execution);
    } catch (Exception e) {
      assertEquals(
          "Raw history is not supported. Please turn off frontend.sendRawWorkflowHistory feature flag in frontend service to recover",
          e.getMessage());
      return;
    }
    fail("Expected exception not thrown");
  }

  @Test(expected = Error.class)
  public void testGetFullHistory_DecodedHistory_ThrowException() throws Exception {
    when(mockServiceClient.GetWorkflowExecutionHistory(any())).thenThrow(new Error());
    activity.getFullHistory(domain, execution);
  }

  @Test
  public void testReplayWorkflowHistory_ExpectedTrue() throws Exception {
    boolean isSuccess =
        activity.replayWorkflowHistory(
            domain,
            execution,
            new WorkflowExecutionHistory(
                Lists.newArrayList(historyEvents.get(0), historyEvents.get(1))));
    assertTrue(isSuccess);
  }

  @Test
  public void testReplayWorkflowHistory_ExpectedFalse() throws Exception {
    HistoryEvent event = historyEvents.get(0);
    WorkflowExecutionStartedEventAttributes attributes =
        event.getWorkflowExecutionStartedEventAttributes();
    attributes.setTaskList(null);
    boolean isSuccess =
        activity.replayWorkflowHistory(
            domain, execution, new WorkflowExecutionHistory(Lists.newArrayList(event)));
    assertFalse(isSuccess);
  }

  @Test(expected = RuntimeException.class)
  public void testReplayWorkflowHistory_ThrowException() throws Exception {
    activity.replayWorkflowHistory(domain, execution, new WorkflowExecutionHistory(historyEvents));
  }

  @Test
  public void testReplay_ExpectedSuccess() throws Exception {
    History history =
        new History().setEvents(Lists.newArrayList(historyEvents.get(0), historyEvents.get(1)));
    GetWorkflowExecutionHistoryResponse response =
        new GetWorkflowExecutionHistoryResponse().setHistory(history);
    when(mockServiceClient.GetWorkflowExecutionHistory(any())).thenReturn(response);
    ReplayWorkflowActivityParams params = new ReplayWorkflowActivityParams();
    params.setDomain(domain);
    params.setExecutions(Lists.newArrayList(execution));
    ReplayWorkflowActivityResult result = activityStub.replay(params);
    assertEquals(1, result.getSucceeded());
  }

  @Test
  public void testReplay_ExpectedSkipped() throws Exception {
    HistoryEvent event = historyEvents.get(0);
    WorkflowExecutionStartedEventAttributes attributes =
        event.getWorkflowExecutionStartedEventAttributes();
    attributes.setTaskList(null);
    History history = new History().setEvents(Lists.newArrayList(historyEvents.get(0)));
    GetWorkflowExecutionHistoryResponse response =
        new GetWorkflowExecutionHistoryResponse().setHistory(history);
    when(mockServiceClient.GetWorkflowExecutionHistory(any())).thenReturn(response);
    ReplayWorkflowActivityParams params = new ReplayWorkflowActivityParams();
    params.setDomain(domain);
    params.setExecutions(Lists.newArrayList(execution));
    ReplayWorkflowActivityResult result = activityStub.replay(params);
    assertEquals(1, result.getSkipped());
  }

  @Test
  public void testReplay_ExpectedFailed() throws Exception {
    History history = new History().setEvents(historyEvents);
    GetWorkflowExecutionHistoryResponse response =
        new GetWorkflowExecutionHistoryResponse().setHistory(history);
    when(mockServiceClient.GetWorkflowExecutionHistory(any())).thenReturn(response);
    ReplayWorkflowActivityParams params = new ReplayWorkflowActivityParams();
    params.setDomain(domain);
    params.setExecutions(Lists.newArrayList(execution));
    ReplayWorkflowActivityResult result = activityStub.replay(params);
    assertEquals(1, result.getFailed());
  }
}
