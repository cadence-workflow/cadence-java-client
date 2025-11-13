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

import com.uber.cadence.EventType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.MarkerRecordedEventAttributes;
import com.uber.cadence.internal.worker.DecisionTaskWithHistoryIterator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionEventsIteratorTest {

  @Mock private DecisionTaskWithHistoryIterator mockDecisionTaskWithHistoryIterator;

  private static final long REPLAY_TIME_MILLIS = 1000L;
  private static final long EVENT_TIMESTAMP_NANOS =
      TimeUnit.MILLISECONDS.toNanos(REPLAY_TIME_MILLIS);

  @Before
  public void setUp() {
    // Default setup - can be overridden in individual tests
  }

  @Test
  public void testConstructor() {
    // Arrange
    List<HistoryEvent> events = Collections.emptyList();
    when(mockDecisionTaskWithHistoryIterator.getHistory()).thenReturn(events.iterator());

    // Act
    HistoryHelper.DecisionEventsIterator iterator =
        new HistoryHelper.DecisionEventsIterator(
            mockDecisionTaskWithHistoryIterator, REPLAY_TIME_MILLIS);

    // Assert
    assertNotNull(iterator);
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testHasNextWithEmptyHistory() {
    // Arrange
    List<HistoryEvent> events = Collections.emptyList();
    when(mockDecisionTaskWithHistoryIterator.getHistory()).thenReturn(events.iterator());

    // Act
    HistoryHelper.DecisionEventsIterator iterator =
        new HistoryHelper.DecisionEventsIterator(
            mockDecisionTaskWithHistoryIterator, REPLAY_TIME_MILLIS);

    // Assert
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testWorkflowStartedOnly() {
    // Arrange
    List<HistoryEvent> events =
        Arrays.asList(
            createHistoryEvent(1, EventType.WorkflowExecutionStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(2, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(3, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS));
    when(mockDecisionTaskWithHistoryIterator.getHistory()).thenReturn(events.iterator());

    // Act
    HistoryHelper.DecisionEventsIterator iterator =
        new HistoryHelper.DecisionEventsIterator(
            mockDecisionTaskWithHistoryIterator, REPLAY_TIME_MILLIS);

    // Assert
    assertTrue(iterator.hasNext());

    HistoryHelper.DecisionEvents decisionEvents = iterator.next();
    assertNotNull(decisionEvents);
    assertEquals(2, decisionEvents.getEvents().size());
    assertEquals(0, decisionEvents.getDecisionEvents().size());
    assertFalse(decisionEvents.isReplay());
    assertEquals(REPLAY_TIME_MILLIS, decisionEvents.getReplayCurrentTimeMilliseconds());
    assertEquals(5, decisionEvents.getNextDecisionEventId());
  }

  @Test
  public void testWorkflowWithActivityScheduled() {
    // Arrange
    List<HistoryEvent> events =
        Arrays.asList(
            createHistoryEvent(1, EventType.WorkflowExecutionStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(2, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(3, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(4, EventType.DecisionTaskCompleted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(5, EventType.ActivityTaskScheduled, EVENT_TIMESTAMP_NANOS));
    when(mockDecisionTaskWithHistoryIterator.getHistory()).thenReturn(events.iterator());

    // Act
    HistoryHelper.DecisionEventsIterator iterator =
        new HistoryHelper.DecisionEventsIterator(
            mockDecisionTaskWithHistoryIterator, REPLAY_TIME_MILLIS);
    HistoryHelper.DecisionEvents decisionEvents = iterator.next();

    // Assert
    assertNotNull(decisionEvents);
    assertEquals(
        2, decisionEvents.getEvents().size()); // WorkflowExecutionStarted and DecisionTaskScheduled
    assertEquals(1, decisionEvents.getDecisionEvents().size()); // ActivityTaskScheduled
    assertTrue(decisionEvents.isReplay());
    assertEquals(REPLAY_TIME_MILLIS, decisionEvents.getReplayCurrentTimeMilliseconds());
    assertEquals(5, decisionEvents.getNextDecisionEventId());
  }

  @Test
  public void testWorkflowWithActivityCompleted() {
    // Arrange - Sticky workers scenario
    List<HistoryEvent> events =
        Arrays.asList(
            createHistoryEvent(1, EventType.WorkflowExecutionStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(2, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(3, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(4, EventType.DecisionTaskCompleted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(5, EventType.ActivityTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(6, EventType.ActivityTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(7, EventType.ActivityTaskCompleted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(8, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(9, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS));
    when(mockDecisionTaskWithHistoryIterator.getHistory()).thenReturn(events.iterator());

    // Act
    HistoryHelper.DecisionEventsIterator iterator =
        new HistoryHelper.DecisionEventsIterator(
            mockDecisionTaskWithHistoryIterator, REPLAY_TIME_MILLIS);

    // first decision batch
    HistoryHelper.DecisionEvents decisionEvents = iterator.next();
    assertNotNull(decisionEvents);
    assertEquals(2, decisionEvents.getEvents().size());
    assertEquals(1, decisionEvents.getDecisionEvents().size());
    assertTrue(decisionEvents.isReplay());
    assertEquals(5, decisionEvents.getNextDecisionEventId());

    // second decision batch
    decisionEvents = iterator.next();
    assertFalse(iterator.hasNext());
    assertEquals(3, decisionEvents.getEvents().size());
    assertEquals(0, decisionEvents.getDecisionEvents().size());
    assertFalse(decisionEvents.isReplay());
    assertEquals(REPLAY_TIME_MILLIS, decisionEvents.getReplayCurrentTimeMilliseconds());
    assertEquals(11, decisionEvents.getNextDecisionEventId());
  }

  @Test
  public void testCompletedWorkflow() {
    // Arrange - Non-replay scenario
    List<HistoryEvent> events =
        Arrays.asList(
            createHistoryEvent(1, EventType.WorkflowExecutionStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(2, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(3, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(4, EventType.DecisionTaskCompleted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(5, EventType.ActivityTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(6, EventType.ActivityTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(7, EventType.ActivityTaskCompleted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(8, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(9, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(10, EventType.DecisionTaskCompleted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(11, EventType.WorkflowExecutionCompleted, EVENT_TIMESTAMP_NANOS));
    when(mockDecisionTaskWithHistoryIterator.getHistory()).thenReturn(events.iterator());

    // Act
    HistoryHelper.DecisionEventsIterator iterator =
        new HistoryHelper.DecisionEventsIterator(
            mockDecisionTaskWithHistoryIterator, REPLAY_TIME_MILLIS);

    // first decision batch
    HistoryHelper.DecisionEvents decisionEvents = iterator.next();
    assertNotNull(decisionEvents);
    assertEquals(2, decisionEvents.getEvents().size());
    assertEquals(1, decisionEvents.getDecisionEvents().size());
    assertTrue(decisionEvents.isReplay());
    assertEquals(5, decisionEvents.getNextDecisionEventId());

    // second decision batch
    decisionEvents = iterator.next();
    assertFalse(iterator.hasNext());
    assertEquals(3, decisionEvents.getEvents().size());
    assertEquals(1, decisionEvents.getDecisionEvents().size());
    assertTrue(decisionEvents.isReplay());
    assertEquals(REPLAY_TIME_MILLIS, decisionEvents.getReplayCurrentTimeMilliseconds());
    assertEquals(11, decisionEvents.getNextDecisionEventId());
  }

  @Test
  public void testNextWithDecisionTaskTimedOut() {
    // Arrange
    List<HistoryEvent> events =
        Arrays.asList(
            createHistoryEvent(1, EventType.WorkflowExecutionStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(2, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(3, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(4, EventType.DecisionTaskTimedOut, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(5, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(6, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(7, EventType.DecisionTaskCompleted, EVENT_TIMESTAMP_NANOS));
    when(mockDecisionTaskWithHistoryIterator.getHistory()).thenReturn(events.iterator());

    // Act
    HistoryHelper.DecisionEventsIterator iterator =
        new HistoryHelper.DecisionEventsIterator(
            mockDecisionTaskWithHistoryIterator, REPLAY_TIME_MILLIS);
    HistoryHelper.DecisionEvents decisionEvents = iterator.next();

    // Assert
    assertNotNull(decisionEvents);
    assertEquals(
        4, decisionEvents.getEvents().size()); // All events before the second DecisionTaskStarted
    assertEquals(0, decisionEvents.getDecisionEvents().size());
    assertTrue(decisionEvents.isReplay());
    assertEquals(8, decisionEvents.getNextDecisionEventId());
  }

  @Test
  public void testNextWithDecisionTaskFailed() {
    // Arrange
    List<HistoryEvent> events =
        Arrays.asList(
            createHistoryEvent(1, EventType.WorkflowExecutionStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(2, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(3, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(4, EventType.DecisionTaskFailed, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(5, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(6, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(7, EventType.DecisionTaskCompleted, EVENT_TIMESTAMP_NANOS));
    when(mockDecisionTaskWithHistoryIterator.getHistory()).thenReturn(events.iterator());

    // Act
    HistoryHelper.DecisionEventsIterator iterator =
        new HistoryHelper.DecisionEventsIterator(
            mockDecisionTaskWithHistoryIterator, REPLAY_TIME_MILLIS);
    HistoryHelper.DecisionEvents decisionEvents = iterator.next();

    // Assert
    assertNotNull(decisionEvents);
    assertEquals(4, decisionEvents.getEvents().size());
    assertEquals(0, decisionEvents.getDecisionEvents().size());
    assertTrue(decisionEvents.isReplay());
    assertEquals(8, decisionEvents.getNextDecisionEventId());
  }

  @Test
  public void testNextWithMarkerRecordedEvents() {
    // Arrange
    HistoryEvent markerEvent =
        createHistoryEvent(5, EventType.MarkerRecorded, EVENT_TIMESTAMP_NANOS);
    markerEvent.setMarkerRecordedEventAttributes(new MarkerRecordedEventAttributes());

    List<HistoryEvent> events =
        Arrays.asList(
            createHistoryEvent(1, EventType.WorkflowExecutionStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(2, EventType.DecisionTaskScheduled, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(3, EventType.DecisionTaskStarted, EVENT_TIMESTAMP_NANOS),
            createHistoryEvent(4, EventType.DecisionTaskCompleted, EVENT_TIMESTAMP_NANOS),
            markerEvent);
    when(mockDecisionTaskWithHistoryIterator.getHistory()).thenReturn(events.iterator());

    // Act
    HistoryHelper.DecisionEventsIterator iterator =
        new HistoryHelper.DecisionEventsIterator(
            mockDecisionTaskWithHistoryIterator, REPLAY_TIME_MILLIS);
    HistoryHelper.DecisionEvents decisionEvents = iterator.next();

    // Assert
    assertNotNull(decisionEvents);
    assertEquals(1, decisionEvents.getMarkers().size());
    assertEquals(markerEvent, decisionEvents.getMarkers().get(0));
  }

  private HistoryEvent createHistoryEvent(long eventId, EventType eventType, long timestamp) {
    HistoryEvent event = new HistoryEvent();
    event.setEventId(eventId);
    event.setEventType(eventType);
    event.setTimestamp(timestamp);
    return event;
  }
}
