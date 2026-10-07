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

import com.google.common.base.Defaults;
import com.google.common.collect.Lists;
import com.uber.cadence.DataBlob;
import com.uber.cadence.History;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.HistoryEventFilterType;
import com.uber.cadence.Memo;
import com.uber.cadence.SearchAttributes;
import com.uber.cadence.TaskList;
import com.uber.cadence.TaskListKind;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.internal.worker.Shutdownable;
import com.uber.cadence.workflow.QueryMethod;
import com.uber.cadence.workflow.SignalMethod;
import com.uber.cadence.workflow.WorkflowMethod;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;

/** Utility functions shared by the implementation code. */
public final class InternalUtils {

  /**
   * Used to construct default name of an activity or workflow type from a method it implements.
   *
   * @return "Simple class name"::"methodName"
   */
  public static String getSimpleName(Method method) {
    return method.getDeclaringClass().getSimpleName() + "::" + method.getName();
  }

  public static String getWorkflowType(Method method, WorkflowMethod workflowMethod) {
    String workflowName = workflowMethod.name();
    if (workflowName.isEmpty()) {
      return InternalUtils.getSimpleName(method);
    } else {
      return workflowName;
    }
  }

  /**
   * Returns the annotation of the given type on a workflow interface method.
   *
   * @param annotationType one of {@link WorkflowMethod}, {@link QueryMethod} or {@link
   *     SignalMethod}
   * @throws IllegalArgumentException if the method has more than one of these annotations, or if
   *     annotationType is not one of them
   */
  public static <T extends Annotation> Optional<T> getWorkflowAnnotation(
      Method method, Class<T> annotationType) {
    if (annotationType != WorkflowMethod.class
        && annotationType != QueryMethod.class
        && annotationType != SignalMethod.class) {
      throw new IllegalArgumentException("Not a workflow method annotation: " + annotationType);
    }
    int count =
        (method.isAnnotationPresent(WorkflowMethod.class) ? 1 : 0)
            + (method.isAnnotationPresent(QueryMethod.class) ? 1 : 0)
            + (method.isAnnotationPresent(SignalMethod.class) ? 1 : 0);
    if (count > 1) {
      throw new IllegalArgumentException(
          method
              + " must contain at most one annotation "
              + "from @WorkflowMethod, @QueryMethod or @SignalMethod");
    }
    return Optional.ofNullable(method.getAnnotation(annotationType));
  }

  public static Method getWorkflowMethod(Class<?> workflowInterface) {
    Method result = null;
    for (Method m : workflowInterface.getMethods()) {
      if (m.getAnnotation(WorkflowMethod.class) != null) {
        if (result != null) {
          throw new IllegalArgumentException(
              "Workflow interface must have exactly one method "
                  + "annotated with @WorkflowMethod. Found \""
                  + result
                  + "\" and \""
                  + m
                  + "\"");
        }
        result = m;
      }
    }
    if (result == null) {
      throw new IllegalArgumentException(
          "Method annotated with @WorkflowMethod is not " + "found at " + workflowInterface);
    }
    return result;
  }

  public static TaskList createStickyTaskList(String taskListName) {
    TaskList tl = new TaskList();
    tl.setName(taskListName);
    tl.setKind(TaskListKind.STICKY);
    return tl;
  }

  public static TaskList createNormalTaskList(String taskListName) {
    TaskList tl = new TaskList();
    tl.setName(taskListName);
    tl.setKind(TaskListKind.NORMAL);
    return tl;
  }

  public static long awaitTermination(Shutdownable s, long timeoutMillis) {
    if (s == null) {
      return timeoutMillis;
    }
    return awaitTermination(
        timeoutMillis,
        () -> {
          s.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
        });
  }

  public static long awaitTermination(ExecutorService s, long timeoutMillis) {
    if (s == null) {
      return timeoutMillis;
    }
    return awaitTermination(
        timeoutMillis,
        () -> {
          try {
            s.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
          }
        });
  }

  public static long awaitTermination(long timeoutMillis, Runnable toTerminate) {
    long started = System.currentTimeMillis();
    toTerminate.run();
    long remainingTimeout = timeoutMillis - (System.currentTimeMillis() - started);
    if (remainingTimeout < 0) {
      remainingTimeout = 0;
    }
    return remainingTimeout;
  }

  public static Object getValueOrDefault(Object value, Class<?> valueClass) {
    if (value != null) {
      return value;
    }
    return Defaults.defaultValue(valueClass);
  }

  public static Memo convertMapToMemo(Map<String, Object> memo) {
    DataConverter converter = JsonDataConverter.getInstance();
    Map<String, ByteBuffer> mapOfByteBuffer = new HashMap<>();
    memo.forEach(
        (key, value) -> {
          mapOfByteBuffer.put(key, ByteBuffer.wrap(converter.toData(value)));
        });
    return new Memo().setFields(mapOfByteBuffer);
  }

  public static SearchAttributes convertMapToSearchAttributes(
      Map<String, Object> searchAttributes) {
    DataConverter converter = JsonDataConverter.getInstance();
    Map<String, ByteBuffer> mapOfByteBuffer = new HashMap<>();
    searchAttributes.forEach(
        (key, value) -> {
          mapOfByteBuffer.put(key, ByteBuffer.wrap(converter.toData(value)));
        });
    return new SearchAttributes().setIndexedFields(mapOfByteBuffer);
  }

  // This method serializes history to blob data
  public static DataBlob SerializeFromHistoryToBlobData(History history) {

    // TODO: move to global dependency after https://issues.apache.org/jira/browse/THRIFT-2218
    TSerializer serializer = new TSerializer();
    DataBlob blob = new DataBlob();
    try {
      blob.setData(serializer.serialize(history));
    } catch (org.apache.thrift.TException err) {
      throw new RuntimeException("Serialize history to blob data failed", err);
    }

    return blob;
  }

  // This method deserialize the DataBlob data to the History data
  public static History DeserializeFromBlobDataToHistory(
      List<DataBlob> blobData, HistoryEventFilterType historyEventFilterType) throws TException {

    // TODO: move to global dependency after https://issues.apache.org/jira/browse/THRIFT-2218
    TDeserializer deSerializer = new TDeserializer();
    List<HistoryEvent> events = Lists.newArrayList();
    for (DataBlob data : blobData) {
      History history = new History();
      try {
        byte[] dataByte = data.getData();
        // TODO: verify the beginning index
        dataByte = Arrays.copyOfRange(dataByte, 0, dataByte.length);
        deSerializer.deserialize(history, dataByte);

        if (history == null || history.getEvents() == null || history.getEvents().size() == 0) {
          return null;
        }
      } catch (org.apache.thrift.TException err) {
        throw new TException("Deserialize blob data to history failed with unknown error");
      }

      events.addAll(history.getEvents());
    }

    if (events.size() > 0 && historyEventFilterType == HistoryEventFilterType.CLOSE_EVENT) {
      events = events.subList(events.size() - 1, events.size());
    }

    return new History().setEvents(events);
  }

  // This method serializes history event to blob data
  public static List<DataBlob> SerializeFromHistoryEventToBlobData(List<HistoryEvent> events) {

    // TODO: move to global dependency after https://issues.apache.org/jira/browse/THRIFT-2218
    TSerializer serializer = new TSerializer();
    List<DataBlob> blobs = Lists.newArrayListWithCapacity(events.size());
    for (HistoryEvent event : events) {
      DataBlob blob = new DataBlob();
      try {
        blob.setData(serializer.serialize(event));
      } catch (org.apache.thrift.TException err) {
        throw new RuntimeException("Serialize history event to blob data failed", err);
      }
      blobs.add(blob);
    }
    return blobs;
  }

  // This method serializes blob data to history event
  public static List<HistoryEvent> DeserializeFromBlobDataToHistoryEvents(List<DataBlob> blobData)
      throws TException {

    // TODO: move to global dependency after https://issues.apache.org/jira/browse/THRIFT-2218
    TDeserializer deSerializer = new TDeserializer();
    List<HistoryEvent> events = Lists.newArrayList();
    for (DataBlob data : blobData) {
      try {
        HistoryEvent event = new HistoryEvent();
        byte[] dataByte = data.getData();
        // TODO: verify the beginning index
        dataByte = Arrays.copyOfRange(dataByte, 0, dataByte.length);
        deSerializer.deserialize(event, dataByte);
        events.add(event);
      } catch (org.apache.thrift.TException err) {
        throw new TException("Deserialize blob data to history event failed with unknown error");
      }
    }
    return events;
  }

  /** Prohibit instantiation */
  private InternalUtils() {}
}
