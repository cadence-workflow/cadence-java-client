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

package com.uber.cadence.internal.replay;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.uber.cadence.*;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.internal.worker.SingleWorkerOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ReplaceDeciderDecisionTaskWithHistoryIteratorTest {
  @Mock private IWorkflowService mockService;

  @Mock private DecisionContextImpl mockContext;

  @Mock private DecisionsHelper mockedHelper;

  private static final int MAXIMUM_PAGE_SIZE = 10000;
  private final String WORKFLOW_ID = "testWorkflowId";
  private final String RUN_ID = "testRunId";
  private final String DOMAIN = "testDomain";
  private final String START_PAGE_TOKEN = "testPageToken";
  private final WorkflowExecution WORKFLOW_EXECUTION =
      new WorkflowExecution().setWorkflowId(WORKFLOW_ID).setRunId(RUN_ID);
  private final HistoryEvent START_EVENT =
      new HistoryEvent()
          .setWorkflowExecutionStartedEventAttributes(new WorkflowExecutionStartedEventAttributes())
          .setEventId(1);
  private final History HISTORY = new History().setEvents(Collections.singletonList(START_EVENT));
  private final PollForDecisionTaskResponse task =
      new PollForDecisionTaskResponse()
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setHistory(HISTORY)
          .setNextPageToken(START_PAGE_TOKEN.getBytes());

  private Object iterator;

  private void setupDecisionTaskWithHistoryIteratorImpl() {
    try {
      // Find the inner class first
      Class<?> innerClass = findDecisionTaskWithHistoryIteratorImplClass();

      // Get the constructor with the specific parameter types
      Constructor<?> constructor =
          innerClass.getDeclaredConstructor(
              ReplayDecider.class, PollForDecisionTaskResponse.class, Duration.class);

      when(mockedHelper.getTask()).thenReturn(task);
      when(mockContext.getDomain()).thenReturn(DOMAIN);

      // Create an instance of the outer class
      ReplayDecider outerInstance =
          new ReplayDecider(
              mockService,
              DOMAIN,
              new WorkflowType().setName("testWorkflow"),
              null,
              mockedHelper,
              SingleWorkerOptions.newBuilder()
                  .setMetricsScope(WorkflowClientOptions.defaultInstance().getMetricsScope())
                  .build(),
              null);

      // Create the instance
      iterator = constructor.newInstance(outerInstance, task, Duration.ofSeconds(10));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to set up test: " + e.getMessage(), e);
    }
  }

  // Helper method to find the inner class
  private Class<?> findDecisionTaskWithHistoryIteratorImplClass() {
    for (Class<?> declaredClass : ReplayDecider.class.getDeclaredClasses()) {
      if (declaredClass.getSimpleName().equals("DecisionTaskWithHistoryIteratorImpl")) {
        return declaredClass;
      }
    }
    throw new RuntimeException("Could not find DecisionTaskWithHistoryIteratorImpl inner class");
  }

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    setupDecisionTaskWithHistoryIteratorImpl();
  }

  @Test
  public void testGetHistoryWithSinglePageOfEvents()
      throws CadenceError, NoSuchMethodException, InvocationTargetException,
          IllegalAccessException {
    // Arrange
    List<HistoryEvent> events = Arrays.asList(createMockHistoryEvent(2), createMockHistoryEvent(3));
    History mockHistory = new History().setEvents(events);
    when(mockService.GetWorkflowExecutionHistory(
            new GetWorkflowExecutionHistoryRequest()
                .setDomain(DOMAIN)
                .setNextPageToken(START_PAGE_TOKEN.getBytes())
                .setExecution(WORKFLOW_EXECUTION)
                .setMaximumPageSize(MAXIMUM_PAGE_SIZE)))
        .thenReturn(new GetWorkflowExecutionHistoryResponse().setHistory(mockHistory));

    // Act & Assert
    Method wrapperMethod = iterator.getClass().getMethod("getHistory");

    Object result = wrapperMethod.invoke(iterator);
    Iterator<HistoryEvent> historyIterator = (Iterator<HistoryEvent>) result;
    assertTrue(historyIterator.hasNext());
    assertEquals(START_EVENT.getEventId(), historyIterator.next().getEventId());
    assertTrue(historyIterator.hasNext());
    assertEquals(events.get(0).getEventId(), historyIterator.next().getEventId());
    assertTrue(historyIterator.hasNext());
    assertEquals(events.get(1).getEventId(), historyIterator.next().getEventId());
    assertFalse(historyIterator.hasNext());
  }

  @Test
  public void testGetHistoryWithMultiplePages()
      throws CadenceError, NoSuchMethodException, InvocationTargetException,
          IllegalAccessException {
    // First page events
    List<HistoryEvent> firstPageEvents =
        Arrays.asList(createMockHistoryEvent(1), createMockHistoryEvent(2));
    History firstHistory = new History().setEvents(firstPageEvents);
    String firstPageToken = "firstPageToken";
    when(mockService.GetWorkflowExecutionHistory(
            eq(
                new GetWorkflowExecutionHistoryRequest()
                    .setDomain(DOMAIN)
                    .setNextPageToken(START_PAGE_TOKEN.getBytes())
                    .setExecution(WORKFLOW_EXECUTION)
                    .setMaximumPageSize(MAXIMUM_PAGE_SIZE))))
        .thenReturn(
            new GetWorkflowExecutionHistoryResponse()
                .setHistory(firstHistory)
                .setNextPageToken(firstPageToken.getBytes()));

    // Second page events
    List<HistoryEvent> secondPageEvents =
        Arrays.asList(createMockHistoryEvent(3), createMockHistoryEvent(4));
    History secondHistory = new History().setEvents(secondPageEvents);
    when(mockService.GetWorkflowExecutionHistory(
            eq(
                new GetWorkflowExecutionHistoryRequest()
                    .setDomain(DOMAIN)
                    .setNextPageToken(firstPageToken.getBytes())
                    .setExecution(WORKFLOW_EXECUTION)
                    .setMaximumPageSize(MAXIMUM_PAGE_SIZE))))
        .thenReturn(new GetWorkflowExecutionHistoryResponse().setHistory(secondHistory));

    // Act & Assert
    Method wrapperMethod = iterator.getClass().getMethod("getHistory");

    Object result = wrapperMethod.invoke(iterator);
    Iterator<HistoryEvent> historyIterator = (Iterator<HistoryEvent>) result;
    // Check first page events
    assertEquals(START_EVENT.getEventId(), historyIterator.next().getEventId());
    assertEquals(firstPageEvents.get(0).getEventId(), historyIterator.next().getEventId());
    assertEquals(firstPageEvents.get(1).getEventId(), historyIterator.next().getEventId());

    // Check second page events
    assertEquals(secondPageEvents.get(0).getEventId(), historyIterator.next().getEventId());
    assertEquals(secondPageEvents.get(1).getEventId(), historyIterator.next().getEventId());

    assertFalse(historyIterator.hasNext());
  }

  @Test(expected = Error.class)
  public void testGetHistoryFailure()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException,
          CadenceError {
    when(mockService.GetWorkflowExecutionHistory(
            new GetWorkflowExecutionHistoryRequest()
                .setDomain(DOMAIN)
                .setNextPageToken(START_PAGE_TOKEN.getBytes())
                .setExecution(WORKFLOW_EXECUTION)
                .setMaximumPageSize(MAXIMUM_PAGE_SIZE)))
        .thenThrow(new CadenceError());

    // Act & Assert
    Method wrapperMethod = iterator.getClass().getMethod("getHistory");

    Object result = wrapperMethod.invoke(iterator);
    Iterator<HistoryEvent> historyIterator = (Iterator<HistoryEvent>) result;
    historyIterator.next();

    historyIterator.next(); // This should throw an Error due to timeout
  }

  @Test(expected = Error.class)
  public void testEmptyHistory()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException,
          CadenceError {
    when(mockService.GetWorkflowExecutionHistory(
            new GetWorkflowExecutionHistoryRequest()
                .setDomain(DOMAIN)
                .setNextPageToken(START_PAGE_TOKEN.getBytes())
                .setExecution(WORKFLOW_EXECUTION)
                .setMaximumPageSize(MAXIMUM_PAGE_SIZE)))
        .thenReturn(
            new GetWorkflowExecutionHistoryResponse()
                .setHistory(new History().setEvents(new ArrayList<>())));

    // Act & Assert
    Method wrapperMethod = iterator.getClass().getMethod("getHistory");

    Object result = wrapperMethod.invoke(iterator);
    Iterator<HistoryEvent> historyIterator = (Iterator<HistoryEvent>) result;
    historyIterator.next();

    historyIterator.next(); // This should throw an Error due to timeout
  }

  // Helper method to create mock HistoryEvent
  private HistoryEvent createMockHistoryEvent(int eventId) {
    return new HistoryEvent().setEventId(eventId);
  }
}
