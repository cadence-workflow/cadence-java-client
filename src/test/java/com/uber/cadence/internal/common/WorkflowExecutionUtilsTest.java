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
package com.uber.cadence.internal.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import com.uber.cadence.*;
import com.uber.cadence.client.WorkflowTerminatedException;
import com.uber.cadence.client.WorkflowTimedOutException;
import com.uber.cadence.internal.compatibility.ClientObjects;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;

public class WorkflowExecutionUtilsTest {

  private IWorkflowService mockService;
  private WorkflowExecution workflowExecution;
  private final String DECISIONS_PRETTY_PRINT =
      "{\n"
          + "  ScheduleActivityTaskDecisionAttributes {\n"
          + "    ActivityId = activityId;\n"
          + "    ActivityType = activityName;\n"
          + "    Domain = domain;\n"
          + "    Header = {\n"
          + "      Fields = { key1=value1, key2=value2 }\n"
          + "    };\n"
          + "    HeartbeatTimeoutSeconds = 4;\n"
          + "    Input = input;\n"
          + "    ScheduleToCloseTimeoutSeconds = 1;\n"
          + "    ScheduleToStartTimeoutSeconds = 2;\n"
          + "    StartToCloseTimeoutSeconds = 3;\n"
          + "    TaskList = taskList\n"
          + "  },\n"
          + "  FailWorkflowExecutionDecisionAttributes {\n"
          + "    Details = {\n"
          + "      \"error\": \"panic\",\n"
          + "      \"stackTrace\": \"fn()\n"
          + "        main()\"\n"
          + "    };\n"
          + "    Reason = failure reason\n"
          + "  },\n"
          + "  null\n"
          + "}";

  @Before
  public void setUp() {
    mockService = mock(IWorkflowService.class);
    workflowExecution =
        new WorkflowExecution().setWorkflowId("testWorkflowId").setRunId("testRunId");
  }

  // Helper method to create a response with a single event
  private GetWorkflowExecutionHistoryResponse createResponseWithEvent(HistoryEvent event) {
    GetWorkflowExecutionHistoryResponse response = new GetWorkflowExecutionHistoryResponse();
    response.setHistory(new History().setEvents(Collections.singletonList(event)));
    return response;
  }

  // ===========================
  // Test for successful completion case
  // ===========================
  @Test
  public void testGetWorkflowExecutionResult_SuccessfulCompletion() throws Exception {
    // Mock a completed event with a result
    HistoryEvent completedEvent = new HistoryEvent();
    completedEvent.setEventType(EventType.WorkflowExecutionCompleted);
    byte[] expectedResult = "result".getBytes();
    completedEvent.setWorkflowExecutionCompletedEventAttributes(
        new com.uber.cadence.WorkflowExecutionCompletedEventAttributes().setResult(expectedResult));

    // Mock the response from GetWorkflowExecutionHistoryWithTimeout
    GetWorkflowExecutionHistoryResponse response = createResponseWithEvent(completedEvent);
    when(mockService.GetWorkflowExecutionHistoryWithTimeout(any(), anyLong())).thenReturn(response);

    byte[] result =
        WorkflowExecutionUtils.getWorkflowExecutionResult(
            mockService, "testDomain", workflowExecution, Optional.empty(), 5, TimeUnit.SECONDS);

    assertArrayEquals(expectedResult, result);
  }

  // ===========================
  // Test for canceled workflow case
  // ===========================
  @Test(expected = CancellationException.class)
  public void testGetWorkflowExecutionResult_CancelledWorkflow() throws Exception {
    // Mock a canceled event
    HistoryEvent canceledEvent = new HistoryEvent();
    canceledEvent.setEventType(EventType.WorkflowExecutionCanceled);
    canceledEvent.setWorkflowExecutionCanceledEventAttributes(
        new com.uber.cadence.WorkflowExecutionCanceledEventAttributes());

    GetWorkflowExecutionHistoryResponse response = createResponseWithEvent(canceledEvent);
    when(mockService.GetWorkflowExecutionHistoryWithTimeout(any(), anyLong())).thenReturn(response);

    WorkflowExecutionUtils.getWorkflowExecutionResult(
        mockService, "testDomain", workflowExecution, Optional.empty(), 5, TimeUnit.SECONDS);
  }

  // ===========================
  // Test for terminated workflow case
  // ===========================
  @Test(expected = WorkflowTerminatedException.class)
  public void testGetWorkflowExecutionResult_TerminatedWorkflow() throws Exception {
    // Mock a terminated event
    HistoryEvent terminatedEvent = new HistoryEvent();
    terminatedEvent.setEventType(EventType.WorkflowExecutionTerminated);
    terminatedEvent.setWorkflowExecutionTerminatedEventAttributes(
        new com.uber.cadence.WorkflowExecutionTerminatedEventAttributes());

    GetWorkflowExecutionHistoryResponse response = createResponseWithEvent(terminatedEvent);
    when(mockService.GetWorkflowExecutionHistoryWithTimeout(any(), anyLong())).thenReturn(response);

    WorkflowExecutionUtils.getWorkflowExecutionResult(
        mockService, "testDomain", workflowExecution, Optional.empty(), 5, TimeUnit.SECONDS);
  }

  // ===========================
  // Test for timed-out workflow case
  // ===========================
  @Test(expected = WorkflowTimedOutException.class)
  public void testGetWorkflowExecutionResult_TimedOutWorkflow() throws Exception {
    // Mock a timed-out event
    HistoryEvent timedOutEvent = new HistoryEvent();
    timedOutEvent.setEventType(EventType.WorkflowExecutionTimedOut);
    timedOutEvent.setWorkflowExecutionTimedOutEventAttributes(
        new com.uber.cadence.WorkflowExecutionTimedOutEventAttributes());

    GetWorkflowExecutionHistoryResponse response = createResponseWithEvent(timedOutEvent);
    when(mockService.GetWorkflowExecutionHistoryWithTimeout(any(), anyLong())).thenReturn(response);

    WorkflowExecutionUtils.getWorkflowExecutionResult(
        mockService, "testDomain", workflowExecution, Optional.empty(), 5, TimeUnit.SECONDS);
  }

  // ===========================
  // Test for an unsupported close event type
  // ===========================
  @Test(expected = RuntimeException.class)
  public void testGetWorkflowExecutionResult_UnsupportedCloseEvent() throws Exception {
    // Mock an event type that doesn't represent a closed workflow
    HistoryEvent unsupportedEvent = new HistoryEvent();
    unsupportedEvent.setEventType(EventType.ActivityTaskScheduled); // Not a close event

    GetWorkflowExecutionHistoryResponse response = createResponseWithEvent(unsupportedEvent);
    when(mockService.GetWorkflowExecutionHistoryWithTimeout(any(), anyLong())).thenReturn(response);

    WorkflowExecutionUtils.getWorkflowExecutionResult(
        mockService, "testDomain", workflowExecution, Optional.empty(), 5, TimeUnit.SECONDS);
  }

  // ===========================
  // Test for timeout exceeded while waiting
  // ===========================
  @Test(expected = TimeoutException.class)
  public void testGetWorkflowExecutionResult_TimeoutExceeded() throws Exception {
    // Simulate a delay by mocking the service call to throw a TimeoutException
    WorkflowExecutionUtils.getWorkflowExecutionResult(
        mockService, "testDomain", workflowExecution, Optional.empty(), -1, TimeUnit.MILLISECONDS);
  }

  // ===========================
  // Test getId with StartChildWorkflowExecutionFailedEvent
  // ===========================
  @Test
  public void testGetId_WithStartChildWorkflowExecutionFailedEvent() {
    // Create a StartChildWorkflowExecutionFailed event with a workflow ID
    HistoryEvent event = new HistoryEvent();
    event.setEventType(EventType.StartChildWorkflowExecutionFailed);
    StartChildWorkflowExecutionFailedEventAttributes attributes =
        new StartChildWorkflowExecutionFailedEventAttributes();
    attributes.setWorkflowId("testWorkflowId");
    event.setStartChildWorkflowExecutionFailedEventAttributes(attributes);

    // Call getId and verify it returns the workflow ID
    String result = WorkflowExecutionUtils.getId(event);
    assertEquals("testWorkflowId", result);
  }

  // ===========================
  // Test getId with other event types (should return null)
  // ===========================
  @Test
  public void testGetId_WithNonChildWorkflowEvent() {
    // Create a HistoryEvent of a different type (e.g., WorkflowExecutionStarted)
    HistoryEvent event = new HistoryEvent();
    event.setEventType(EventType.WorkflowExecutionStarted);

    // Call getId and verify it returns null
    String result = WorkflowExecutionUtils.getId(event);
    assertNull(result);
  }

  @Test
  public void testGetId_WithNullEvent() {
    // Call getId with a null event and verify it returns null
    String result = WorkflowExecutionUtils.getId(null);
    assertNull(result);
  }

  // ===========================
  // Test getCloseStatus for each close event type
  // ===========================

  @Test
  public void testGetCloseStatus_Completed() {
    HistoryEvent event = new HistoryEvent();
    event.setEventType(EventType.WorkflowExecutionCompleted);

    WorkflowExecutionCloseStatus result = WorkflowExecutionUtils.getCloseStatus(event);
    assertEquals(WorkflowExecutionCloseStatus.COMPLETED, result);
  }

  @Test
  public void testGetCloseStatus_Canceled() {
    HistoryEvent event = new HistoryEvent();
    event.setEventType(EventType.WorkflowExecutionCanceled);

    WorkflowExecutionCloseStatus result = WorkflowExecutionUtils.getCloseStatus(event);
    assertEquals(WorkflowExecutionCloseStatus.CANCELED, result);
  }

  @Test
  public void testGetCloseStatus_Failed() {
    HistoryEvent event = new HistoryEvent();
    event.setEventType(EventType.WorkflowExecutionFailed);

    WorkflowExecutionCloseStatus result = WorkflowExecutionUtils.getCloseStatus(event);
    assertEquals(WorkflowExecutionCloseStatus.FAILED, result);
  }

  @Test
  public void testGetCloseStatus_TimedOut() {
    HistoryEvent event = new HistoryEvent();
    event.setEventType(EventType.WorkflowExecutionTimedOut);

    WorkflowExecutionCloseStatus result = WorkflowExecutionUtils.getCloseStatus(event);
    assertEquals(WorkflowExecutionCloseStatus.TIMED_OUT, result);
  }

  @Test
  public void testGetCloseStatus_ContinuedAsNew() {
    HistoryEvent event = new HistoryEvent();
    event.setEventType(EventType.WorkflowExecutionContinuedAsNew);

    WorkflowExecutionCloseStatus result = WorkflowExecutionUtils.getCloseStatus(event);
    assertEquals(WorkflowExecutionCloseStatus.CONTINUED_AS_NEW, result);
  }

  @Test
  public void testGetCloseStatus_Terminated() {
    HistoryEvent event = new HistoryEvent();
    event.setEventType(EventType.WorkflowExecutionTerminated);

    WorkflowExecutionCloseStatus result = WorkflowExecutionUtils.getCloseStatus(event);
    assertEquals(WorkflowExecutionCloseStatus.TERMINATED, result);
  }

  // ===========================
  // Test getCloseStatus with an unsupported event type
  // ===========================
  @Test
  public void testGetCloseStatus_InvalidEvent() {
    HistoryEvent event = new HistoryEvent();
    event.setEventType(EventType.ActivityTaskScheduled); // Unsupported close event type

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              WorkflowExecutionUtils.getCloseStatus(event);
            });

    assertTrue(exception.getMessage().contains("Not close event"));
  }

  // Helper method to create a response with events and a next page token
  private GetWorkflowExecutionHistoryResponse createResponseWithEvents(
      HistoryEvent[] events, byte[] nextPageToken) {
    GetWorkflowExecutionHistoryResponse response = new GetWorkflowExecutionHistoryResponse();
    response.setHistory(new History().setEvents(Arrays.asList(events)));
    response.setNextPageToken(nextPageToken);
    return response;
  }
  // ===========================
  // Test for single-page history
  // ===========================
  @Test
  public void testGetHistory_SinglePage() throws Exception {
    // Create a single-page history with two events
    HistoryEvent event1 =
        new HistoryEvent().setEventType(EventType.WorkflowExecutionStarted).setEventId(1);
    HistoryEvent event2 =
        new HistoryEvent().setEventType(EventType.WorkflowExecutionCompleted).setEventId(2);

    GetWorkflowExecutionHistoryResponse response =
        createResponseWithEvents(new HistoryEvent[] {event1, event2}, null);
    when(mockService.GetWorkflowExecutionHistory(any())).thenReturn(response);

    Iterator<HistoryEvent> iterator =
        WorkflowExecutionUtils.getHistory(mockService, "testDomain", workflowExecution);

    assertTrue(iterator.hasNext());
    assertEquals(event1, iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals(event2, iterator.next());
    assertFalse(iterator.hasNext()); // End of history
  }

  // ===========================
  // Test for multi-page history
  // ===========================
  @Test
  public void testGetHistory_MultiPage() throws Exception {
    // Create a multi-page history with three events over two pages
    HistoryEvent event1 =
        new HistoryEvent().setEventType(EventType.WorkflowExecutionStarted).setEventId(1);
    HistoryEvent event2 =
        new HistoryEvent().setEventType(EventType.ActivityTaskScheduled).setEventId(2);
    HistoryEvent event3 =
        new HistoryEvent().setEventType(EventType.WorkflowExecutionCompleted).setEventId(3);

    // Page 1 with a nextPageToken
    byte[] nextPageToken = new byte[] {1};
    GetWorkflowExecutionHistoryResponse responsePage1 =
        createResponseWithEvents(new HistoryEvent[] {event1, event2}, nextPageToken);

    // Page 2 with no nextPageToken, indicating the end of history
    GetWorkflowExecutionHistoryResponse responsePage2 =
        createResponseWithEvents(new HistoryEvent[] {event3}, null);

    when(mockService.GetWorkflowExecutionHistory(any()))
        .thenReturn(responsePage1) // First call returns page 1
        .thenReturn(responsePage2); // Second call returns page 2

    Iterator<HistoryEvent> iterator =
        WorkflowExecutionUtils.getHistory(mockService, "testDomain", workflowExecution);

    // Verify the iterator iterates over both pages
    assertTrue(iterator.hasNext());
    assertEquals(event1, iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals(event2, iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals(event3, iterator.next());
    assertFalse(iterator.hasNext()); // End of history
  }

  // ===========================
  // Test for empty history
  // ===========================
  @Test
  public void testGetHistory_EmptyHistory() throws Exception {
    // Create an empty history response
    GetWorkflowExecutionHistoryResponse response =
        createResponseWithEvents(new HistoryEvent[] {}, null);
    when(mockService.GetWorkflowExecutionHistory(any())).thenReturn(response);

    Iterator<HistoryEvent> iterator =
        WorkflowExecutionUtils.getHistory(mockService, "testDomain", workflowExecution);

    // Verify that hasNext() is false immediately
    assertFalse(iterator.hasNext());
  }

  // ===========================
  // Test for null history returned to GetHistoryPage
  // ===========================
  @Test
  public void testGetHistoryPage_HistoryIsNull() throws Exception {
    when(mockService.GetWorkflowExecutionHistory(any())).thenReturn(null);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              WorkflowExecutionUtils.getHistoryPage(
                  "page-token".getBytes(), mockService, "testDomain", workflowExecution);
            });

    assertTrue(exception.getMessage().contains(workflowExecution.toString()));
  }

  // ===========================
  // Test for exception thrown on GetWorkflowExecutionHistory
  // ===========================
  @Test
  public void testGetHistoryPage_ExceptionWhileRetrievingExecutionHistory() throws Exception {
    final String errMessage = "comm exception";
    when(mockService.GetWorkflowExecutionHistory(any())).thenThrow(new CadenceError(errMessage));

    Error exception =
        assertThrows(
            Error.class,
            () -> {
              WorkflowExecutionUtils.getHistoryPage(
                  "page-token".getBytes(), mockService, "testDomain", workflowExecution);
            });

    assertTrue(exception.getMessage().contains(errMessage));
  }

  @Test
  public void testPrettyPrintDecisions() throws Exception {
    final TaskList taskList =
        new com.uber.cadence.TaskList()
            .setName("taskList")
            .setKind(com.uber.cadence.TaskListKind.NORMAL);

    final ActivityType activityType = new ActivityType().setName("activityName");
    final Header activityHeader =
        new Header()
            .setFields(
                ImmutableMap.of(
                    "key1", ClientObjects.utf8("value1"),
                    "key2", ClientObjects.utf8("value2")));

    final Decision decisionScheduleActivity =
        new Decision()
            .setDecisionType(DecisionType.ScheduleActivityTask)
            .setScheduleActivityTaskDecisionAttributes(
                new ScheduleActivityTaskDecisionAttributes()
                    .setActivityId("activityId")
                    .setActivityType(activityType)
                    .setTaskList(taskList)
                    .setInput(ClientObjects.utf8("input"))
                    .setScheduleToCloseTimeoutSeconds(1)
                    .setScheduleToStartTimeoutSeconds(2)
                    .setStartToCloseTimeoutSeconds(3)
                    .setHeartbeatTimeoutSeconds(4)
                    .setHeader(activityHeader)
                    .setRequestLocalDispatch(true)
                    .setDomain("domain"));

    final Decision decisionFailWorkflow =
        new Decision()
            .setDecisionType(DecisionType.FailWorkflowExecution)
            .setFailWorkflowExecutionDecisionAttributes(
                new FailWorkflowExecutionDecisionAttributes()
                    .setReason("failure reason")
                    .setDetails(
                        ClientObjects.utf8(
                            "{\"error\":\"panic\", \"stackTrace\":\"fn()\\nmain()\"}")));

    ArrayList<Decision> decisions =
        new ArrayList<>(Arrays.asList(decisionScheduleActivity, decisionFailWorkflow, null));

    String result = WorkflowExecutionUtils.prettyPrintDecisions(decisions);
    assertEquals(DECISIONS_PRETTY_PRINT, result);
  }
}
