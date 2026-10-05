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

package com.uber.cadence.internal.common;

import static com.uber.cadence.EventType.WorkflowExecutionStarted;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.Lists;
import com.googlecode.junittoolbox.MultithreadingTester;
import com.googlecode.junittoolbox.RunnableAssert;
import com.uber.cadence.*;
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.converter.DataConverterException;
import com.uber.cadence.workflow.QueryMethod;
import com.uber.cadence.workflow.SignalMethod;
import com.uber.cadence.workflow.WorkflowMethod;
import com.uber.cadence.workflow.WorkflowUtils;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import junit.framework.TestCase;
import org.junit.Test;

public class InternalUtilsTest {

  @Test
  public void testConvertMapToSearchAttributes() throws Throwable {
    Map<String, Object> attr = new HashMap<>();
    String value = "keyword";
    attr.put("CustomKeywordField", value);

    SearchAttributes result = InternalUtils.convertMapToSearchAttributes(attr);
    assertEquals(
        value,
        WorkflowUtils.getValueFromSearchAttributes(result, "CustomKeywordField", String.class));
  }

  @Test(expected = DataConverterException.class)
  public void testConvertMapToSearchAttributesException() throws Throwable {
    Map<String, Object> attr = new HashMap<>();
    attr.put("InvalidValue", new FileOutputStream("dummy"));
    InternalUtils.convertMapToSearchAttributes(attr);
  }

  @Test
  public void testSerialization_History() {

    RunnableAssert r =
        new RunnableAssert("history_serialization") {
          @Override
          public void run() {
            HistoryEvent event =
                new HistoryEvent()
                    .setEventId(1)
                    .setVersion(1)
                    .setEventType(WorkflowExecutionStarted)
                    .setTimestamp(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                    .setWorkflowExecutionStartedEventAttributes(
                        new WorkflowExecutionStartedEventAttributes()
                            .setAttempt(1)
                            .setFirstExecutionRunId("test"));

            List<HistoryEvent> historyEvents = Lists.newArrayList(event);
            History history = new History().setEvents(historyEvents);
            DataBlob blob = InternalUtils.SerializeFromHistoryToBlobData(history);
            assertNotNull(blob);

            try {
              History result =
                  InternalUtils.DeserializeFromBlobDataToHistory(
                      Lists.newArrayList(blob), HistoryEventFilterType.ALL_EVENT);
              assertNotNull(result);
              assertEquals(1, result.events.size());
              assertEquals(event.getEventId(), result.events.get(0).getEventId());
              assertEquals(event.getVersion(), result.events.get(0).getVersion());
              assertEquals(event.getEventType(), result.events.get(0).getEventType());
              assertEquals(event.getTimestamp(), result.events.get(0).getTimestamp());
              assertEquals(
                  event.getWorkflowExecutionStartedEventAttributes(),
                  result.events.get(0).getWorkflowExecutionStartedEventAttributes());
            } catch (Exception e) {
              TestCase.fail("Received unexpected error during deserialization");
            }
          }
        };

    try {
      new MultithreadingTester().add(r).numThreads(50).numRoundsPerThread(10).run();
    } catch (Exception e) {
      TestCase.fail("Received unexpected error during concurrent deserialization");
    }
  }

  @Test
  public void testSerialization_HistoryEvent() {

    RunnableAssert r =
        new RunnableAssert("history_event_serialization") {
          @Override
          public void run() {
            HistoryEvent event =
                new HistoryEvent()
                    .setEventId(1)
                    .setVersion(1)
                    .setEventType(WorkflowExecutionStarted)
                    .setTimestamp(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                    .setWorkflowExecutionStartedEventAttributes(
                        new WorkflowExecutionStartedEventAttributes()
                            .setAttempt(1)
                            .setFirstExecutionRunId("test"));

            List<HistoryEvent> historyEvents = Lists.newArrayList(event);
            List<DataBlob> blobList =
                InternalUtils.SerializeFromHistoryEventToBlobData(historyEvents);
            assertEquals(1, blobList.size());

            try {
              List<HistoryEvent> result =
                  InternalUtils.DeserializeFromBlobDataToHistoryEvents(blobList);
              assertNotNull(result);
              assertEquals(1, result.size());
              assertEquals(event.getEventId(), result.get(0).getEventId());
              assertEquals(event.getVersion(), result.get(0).getVersion());
              assertEquals(event.getEventType(), result.get(0).getEventType());
              assertEquals(event.getTimestamp(), result.get(0).getTimestamp());
              assertEquals(
                  event.getWorkflowExecutionStartedEventAttributes(),
                  result.get(0).getWorkflowExecutionStartedEventAttributes());
            } catch (Exception e) {
              TestCase.fail("Received unexpected error during deserialization");
            }
          }
        };

    try {
      new MultithreadingTester().add(r).numThreads(50).numRoundsPerThread(10).run();
    } catch (Exception e) {
      TestCase.fail("Received unexpected error during concurrent deserialization");
    }
  }

  public interface AnnotatedWorkflow {
    @WorkflowMethod(name = "workflowType")
    void workflow();

    @QueryMethod(name = "queryType")
    String query();

    @SignalMethod(name = "signalType")
    void signal();

    void notAnnotated();

    @WorkflowMethod
    @SignalMethod
    void workflowAndSignal();

    @QueryMethod
    @SignalMethod
    void queryAndSignal();
  }

  private static Method annotatedWorkflowMethod(String name) throws NoSuchMethodException {
    return AnnotatedWorkflow.class.getMethod(name);
  }

  @Test
  public void testGetWorkflowAnnotation() throws Exception {
    Method workflow = annotatedWorkflowMethod("workflow");
    assertEquals(
        "workflowType",
        InternalUtils.getWorkflowAnnotation(workflow, WorkflowMethod.class).get().name());
    assertFalse(InternalUtils.getWorkflowAnnotation(workflow, QueryMethod.class).isPresent());
    assertFalse(InternalUtils.getWorkflowAnnotation(workflow, SignalMethod.class).isPresent());

    assertEquals(
        "queryType",
        InternalUtils.getWorkflowAnnotation(annotatedWorkflowMethod("query"), QueryMethod.class)
            .get()
            .name());
    assertEquals(
        "signalType",
        InternalUtils.getWorkflowAnnotation(annotatedWorkflowMethod("signal"), SignalMethod.class)
            .get()
            .name());

    Method notAnnotated = annotatedWorkflowMethod("notAnnotated");
    assertEquals(
        Optional.empty(), InternalUtils.getWorkflowAnnotation(notAnnotated, WorkflowMethod.class));
  }

  @Test
  public void testGetWorkflowAnnotationRejectsMultipleAnnotations() throws Exception {
    Method workflowAndSignal = annotatedWorkflowMethod("workflowAndSignal");
    assertThrows(
        IllegalArgumentException.class,
        () -> InternalUtils.getWorkflowAnnotation(workflowAndSignal, WorkflowMethod.class));
    // Rejected even when the requested annotation is absent.
    assertThrows(
        IllegalArgumentException.class,
        () -> InternalUtils.getWorkflowAnnotation(workflowAndSignal, QueryMethod.class));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            InternalUtils.getWorkflowAnnotation(
                annotatedWorkflowMethod("queryAndSignal"), SignalMethod.class));
  }

  @Test
  public void testGetWorkflowAnnotationRejectsOtherAnnotationTypes() throws Exception {
    Method workflow = annotatedWorkflowMethod("workflow");
    assertThrows(
        IllegalArgumentException.class,
        () -> InternalUtils.getWorkflowAnnotation(workflow, ActivityMethod.class));
  }
}
