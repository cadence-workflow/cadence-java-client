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
package com.uber.cadence.internal.compatibility.thrift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.uber.cadence.EventType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.internal.compatibility.MapperTestUtil;
import com.uber.cadence.internal.compatibility.ProtoObjects;
import com.uber.cadence.internal.compatibility.ThriftObjects;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HistoryMapperEventTest<
    F extends Message, E extends Enum<E> & TFieldIdEnum, T extends TBase<T, E>> {
  private static final Map<Class<? extends Message>, Method> ATTRIBUTE_CONVERTERS =
      indexSingleParameterMethods(HistoryMapper.class, Message.class);
  private static final long EVENT_ID = 1;
  private static final int TIMESTAMP_NANOS = 100;
  private static final Timestamp TIMESTAMP =
      Timestamp.newBuilder().setNanos(TIMESTAMP_NANOS).build();
  private static final long VERSION = 2;
  private static final long TASK_ID = 3;

  @Parameterized.Parameter public EventType eventType;

  @Parameterized.Parameter(1)
  public F from;

  @Parameterized.Parameter(2)
  public T to;

  @Parameterized.Parameter(3)
  public Set<String> missingFields;

  @Test
  public void testHistoryEvent() {
    HistoryEvent thriftEvent = createThriftHistoryEvent();
    assertEquals(thriftEvent, HistoryMapper.historyEvent(createProtoHistoryEvent()));
  }

  @Test
  public void testConverter() {
    assertEquals(to, convertToThrift(from));
  }

  @Test
  public void testConverterWithNull() {
    assertNull("Passing null should return null", convertToThrift(null));
  }

  @Test
  public void testAllFieldsPresent() {
    if (missingFields.isEmpty()) {
      MapperTestUtil.assertNoMissingFields(to);
    } else {
      MapperTestUtil.assertMissingFields(to, missingFields);
    }
  }

  private com.uber.cadence.api.v1.HistoryEvent createProtoHistoryEvent() {
    com.uber.cadence.api.v1.HistoryEvent.Builder eventBuilder =
        com.uber.cadence.api.v1.HistoryEvent.newBuilder()
            .setEventId(EVENT_ID)
            .setEventTime(TIMESTAMP)
            .setVersion(VERSION)
            .setTaskId(TASK_ID);
    // Apply the attributes
    callSetter(eventBuilder, from);
    return eventBuilder.build();
  }

  private HistoryEvent createThriftHistoryEvent() {
    HistoryEvent event =
        new HistoryEvent()
            .setEventId(EVENT_ID)
            .setEventType(eventType)
            .setTimestamp(TIMESTAMP_NANOS)
            .setVersion(VERSION)
            .setTaskId(TASK_ID);
    // Apply the attributes
    callSetter(event, to);
    return event;
  }

  private Object convertToThrift(F proto) {
    Method converter = ATTRIBUTE_CONVERTERS.get(from.getClass());
    Preconditions.checkState(
        converter != null, "failed to find converter for %s", from.getClass().getSimpleName());
    try {
      return converter.invoke(null, proto);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private static void callSetter(Object target, Object toSet) {
    for (Method method : target.getClass().getDeclaredMethods()) {
      Class<?>[] params = method.getParameterTypes();
      if (method.getName().startsWith("set")
          && params.length == 1
          && toSet.getClass().isAssignableFrom(params[0])) {
        try {
          method.invoke(target, toSet);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  // Index all the functions in HistoryMapper by parameter type so we can find them dynamically
  private static <V> Map<Class<? extends V>, Method> indexSingleParameterMethods(
      Class<?> target, Class<V> parameterType) {
    Map<Class<? extends V>, Method> byParameterType = new HashMap<>();
    for (Method method : target.getDeclaredMethods()) {
      Class<?>[] params = method.getParameterTypes();
      if (params.length == 1 && parameterType.isAssignableFrom(params[0])) {
        Class<? extends V> protoType = (Class<? extends V>) params[0];
        byParameterType.put(protoType, method);
      }
    }
    return byParameterType;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Object[][] cases() {
    return new Object[][] {
      testCase(
          EventType.WorkflowExecutionStarted,
          ProtoObjects.WORKFLOW_EXECUTION_STARTED_EVENT_ATTRIBUTES,
          ThriftObjects.WORKFLOW_EXECUTION_STARTED_EVENT_ATTRIBUTES,
          "firstScheduledTimeNano",
          "partitionConfig",
          "requestId"),
      testCase(
          EventType.WorkflowExecutionCompleted,
          ProtoObjects.WORKFLOW_EXECUTION_COMPLETED_EVENT_ATTRIBUTES,
          ThriftObjects.WORKFLOW_EXECUTION_COMPLETED_EVENT_ATTRIBUTES),
      testCase(
          EventType.WorkflowExecutionFailed,
          ProtoObjects.WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES,
          ThriftObjects.WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES),
      testCase(
          EventType.WorkflowExecutionTimedOut,
          ProtoObjects.WORKFLOW_EXECUTION_TIMED_OUT_EVENT_ATTRIBUTES,
          ThriftObjects.WORKFLOW_EXECUTION_TIMED_OUT_EVENT_ATTRIBUTES),
      testCase(
          EventType.DecisionTaskScheduled,
          ProtoObjects.DECISION_TASK_SCHEDULED_EVENT_ATTRIBUTES,
          ThriftObjects.DECISION_TASK_SCHEDULED_EVENT_ATTRIBUTES),
      testCase(
          EventType.DecisionTaskStarted,
          ProtoObjects.DECISION_TASK_STARTED_EVENT_ATTRIBUTES,
          ThriftObjects.DECISION_TASK_STARTED_EVENT_ATTRIBUTES),
      testCase(
          EventType.DecisionTaskCompleted,
          ProtoObjects.DECISION_TASK_COMPLETED_EVENT_ATTRIBUTES,
          ThriftObjects.DECISION_TASK_COMPLETED_EVENT_ATTRIBUTES),
      testCase(
          EventType.DecisionTaskTimedOut,
          ProtoObjects.DECISION_TASK_TIMED_OUT_EVENT_ATTRIBUTES,
          ThriftObjects.DECISION_TASK_TIMED_OUT_EVENT_ATTRIBUTES,
          "requestId"),
      testCase(
          EventType.DecisionTaskFailed,
          ProtoObjects.DECISION_TASK_FAILED_EVENT_ATTRIBUTES,
          ThriftObjects.DECISION_TASK_FAILED_EVENT_ATTRIBUTES,
          "requestId"),
      testCase(
          EventType.ActivityTaskScheduled,
          ProtoObjects.ACTIVITY_TASK_SCHEDULED_EVENT_ATTRIBUTES,
          ThriftObjects.ACTIVITY_TASK_SCHEDULED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ActivityTaskStarted,
          ProtoObjects.ACTIVITY_TASK_STARTED_EVENT_ATTRIBUTES,
          ThriftObjects.ACTIVITY_TASK_STARTED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ActivityTaskCompleted,
          ProtoObjects.ACTIVITY_TASK_COMPLETED_EVENT_ATTRIBUTES,
          ThriftObjects.ACTIVITY_TASK_COMPLETED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ActivityTaskFailed,
          ProtoObjects.ACTIVITY_TASK_FAILED_EVENT_ATTRIBUTES,
          ThriftObjects.ACTIVITY_TASK_FAILED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ActivityTaskTimedOut,
          ProtoObjects.ACTIVITY_TASK_TIMED_OUT_EVENT_ATTRIBUTES,
          ThriftObjects.ACTIVITY_TASK_TIMED_OUT_EVENT_ATTRIBUTES),
      testCase(
          EventType.ActivityTaskCancelRequested,
          ProtoObjects.ACTIVITY_TASK_CANCEL_REQUESTED_EVENT_ATTRIBUTES,
          ThriftObjects.ACTIVITY_TASK_CANCEL_REQUESTED_EVENT_ATTRIBUTES),
      testCase(
          EventType.RequestCancelActivityTaskFailed,
          ProtoObjects.REQUEST_CANCEL_ACTIVITY_TASK_FAILED_EVENT_ATTRIBUTES,
          ThriftObjects.REQUEST_CANCEL_ACTIVITY_TASK_FAILED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ActivityTaskCanceled,
          ProtoObjects.ACTIVITY_TASK_CANCELED_EVENT_ATTRIBUTES,
          ThriftObjects.ACTIVITY_TASK_CANCELED_EVENT_ATTRIBUTES),
      testCase(
          EventType.TimerStarted,
          ProtoObjects.TIMER_STARTED_EVENT_ATTRIBUTES,
          ThriftObjects.TIMER_STARTED_EVENT_ATTRIBUTES),
      testCase(
          EventType.TimerFired,
          ProtoObjects.TIMER_FIRED_EVENT_ATTRIBUTES,
          ThriftObjects.TIMER_FIRED_EVENT_ATTRIBUTES),
      testCase(
          EventType.CancelTimerFailed,
          ProtoObjects.CANCEL_TIMER_FAILED_EVENT_ATTRIBUTES,
          ThriftObjects.CANCEL_TIMER_FAILED_EVENT_ATTRIBUTES),
      testCase(
          EventType.TimerCanceled,
          ProtoObjects.TIMER_CANCELED_EVENT_ATTRIBUTES,
          ThriftObjects.TIMER_CANCELED_EVENT_ATTRIBUTES),
      testCase(
          EventType.WorkflowExecutionCancelRequested,
          ProtoObjects.WORKFLOW_EXECUTION_CANCEL_REQUESTED_EVENT_ATTRIBUTES,
          ThriftObjects.WORKFLOW_EXECUTION_CANCEL_REQUESTED_EVENT_ATTRIBUTES,
          "requestId"),
      testCase(
          EventType.WorkflowExecutionCanceled,
          ProtoObjects.WORKFLOW_EXECUTION_CANCELED_EVENT_ATTRIBUTES,
          ThriftObjects.WORKFLOW_EXECUTION_CANCELED_EVENT_ATTRIBUTES),
      testCase(
          EventType.RequestCancelExternalWorkflowExecutionInitiated,
          ProtoObjects.REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_INITIATED_EVENT_ATTRIBUTES,
          ThriftObjects.REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_INITIATED_EVENT_ATTRIBUTES),
      testCase(
          EventType.RequestCancelExternalWorkflowExecutionFailed,
          ProtoObjects.REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES,
          ThriftObjects.REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ExternalWorkflowExecutionCancelRequested,
          ProtoObjects.EXTERNAL_WORKFLOW_EXECUTION_CANCEL_REQUESTED_EVENT_ATTRIBUTES,
          ThriftObjects.EXTERNAL_WORKFLOW_EXECUTION_CANCEL_REQUESTED_EVENT_ATTRIBUTES),
      testCase(
          EventType.MarkerRecorded,
          ProtoObjects.MARKER_RECORDED_EVENT_ATTRIBUTES,
          ThriftObjects.MARKER_RECORDED_EVENT_ATTRIBUTES),
      testCase(
          EventType.WorkflowExecutionSignaled,
          ProtoObjects.WORKFLOW_EXECUTION_SIGNALED_EVENT_ATTRIBUTES,
          ThriftObjects.WORKFLOW_EXECUTION_SIGNALED_EVENT_ATTRIBUTES,
          "requestId"),
      testCase(
          EventType.WorkflowExecutionTerminated,
          ProtoObjects.WORKFLOW_EXECUTION_TERMINATED_EVENT_ATTRIBUTES,
          ThriftObjects.WORKFLOW_EXECUTION_TERMINATED_EVENT_ATTRIBUTES),
      testCase(
          EventType.WorkflowExecutionContinuedAsNew,
          ProtoObjects.WORKFLOW_EXECUTION_CONTINUED_AS_NEW_EVENT_ATTRIBUTES,
          ThriftObjects.WORKFLOW_EXECUTION_CONTINUED_AS_NEW_EVENT_ATTRIBUTES),
      testCase(
          EventType.StartChildWorkflowExecutionInitiated,
          ProtoObjects.START_CHILD_WORKFLOW_EXECUTION_INITIATED_EVENT_ATTRIBUTES,
          ThriftObjects.START_CHILD_WORKFLOW_EXECUTION_INITIATED_EVENT_ATTRIBUTES),
      testCase(
          EventType.StartChildWorkflowExecutionFailed,
          ProtoObjects.START_CHILD_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES,
          ThriftObjects.START_CHILD_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ChildWorkflowExecutionStarted,
          ProtoObjects.CHILD_WORKFLOW_EXECUTION_STARTED_EVENT_ATTRIBUTES,
          ThriftObjects.CHILD_WORKFLOW_EXECUTION_STARTED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ChildWorkflowExecutionCompleted,
          ProtoObjects.CHILD_WORKFLOW_EXECUTION_COMPLETED_EVENT_ATTRIBUTES,
          ThriftObjects.CHILD_WORKFLOW_EXECUTION_COMPLETED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ChildWorkflowExecutionFailed,
          ProtoObjects.CHILD_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES,
          ThriftObjects.CHILD_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ChildWorkflowExecutionCanceled,
          ProtoObjects.CHILD_WORKFLOW_EXECUTION_CANCELED_EVENT_ATTRIBUTES,
          ThriftObjects.CHILD_WORKFLOW_EXECUTION_CANCELED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ChildWorkflowExecutionTimedOut,
          ProtoObjects.CHILD_WORKFLOW_EXECUTION_TIMED_OUT_EVENT_ATTRIBUTES,
          ThriftObjects.CHILD_WORKFLOW_EXECUTION_TIMED_OUT_EVENT_ATTRIBUTES),
      testCase(
          EventType.ChildWorkflowExecutionTerminated,
          ProtoObjects.CHILD_WORKFLOW_EXECUTION_TERMINATED_EVENT_ATTRIBUTES,
          ThriftObjects.CHILD_WORKFLOW_EXECUTION_TERMINATED_EVENT_ATTRIBUTES),
      testCase(
          EventType.SignalExternalWorkflowExecutionInitiated,
          ProtoObjects.SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_INITIATED_EVENT_ATTRIBUTES,
          ThriftObjects.SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_INITIATED_EVENT_ATTRIBUTES),
      testCase(
          EventType.SignalExternalWorkflowExecutionFailed,
          ProtoObjects.SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES,
          ThriftObjects.SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES),
      testCase(
          EventType.ExternalWorkflowExecutionSignaled,
          ProtoObjects.EXTERNAL_WORKFLOW_EXECUTION_SIGNALED_EVENT_ATTRIBUTES,
          ThriftObjects.EXTERNAL_WORKFLOW_EXECUTION_SIGNALED_EVENT_ATTRIBUTES),
      testCase(
          EventType.UpsertWorkflowSearchAttributes,
          ProtoObjects.UPSERT_WORKFLOW_SEARCH_ATTRIBUTES_EVENT_ATTRIBUTES,
          ThriftObjects.UPSERT_WORKFLOW_SEARCH_ATTRIBUTES_EVENT_ATTRIBUTES),
    };
  }

  private static Object[] testCase(
      EventType type, Message proto, TBase<?, ?> thrift, String... expectedMissingFields) {
    return new Object[] {type, proto, thrift, ImmutableSet.copyOf(expectedMissingFields)};
  }
}
