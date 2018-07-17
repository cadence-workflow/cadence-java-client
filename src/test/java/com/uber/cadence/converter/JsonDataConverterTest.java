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

package com.uber.cadence.converter;

import static org.junit.Assert.assertEquals;

import com.uber.cadence.History;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.TaskList;
import com.uber.cadence.WorkflowExecutionStartedEventAttributes;
import com.uber.cadence.WorkflowType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class JsonDataConverterTest {

  private final DataConverter converter = JsonDataConverter.getInstance();

  @Test
  public void testThrift() {
    List<HistoryEvent> events = new ArrayList<>();
    WorkflowExecutionStartedEventAttributes started =
        new WorkflowExecutionStartedEventAttributes()
            .setExecutionStartToCloseTimeoutSeconds(11)
            .setIdentity("testIdentity")
            .setInput("input".getBytes(StandardCharsets.UTF_8))
            .setWorkflowType(new WorkflowType().setName("workflowType1"))
            .setTaskList(new TaskList().setName("taskList1"));
    events.add(
        new HistoryEvent()
            .setTimestamp(1234567)
            .setEventId(321)
            .setWorkflowExecutionStartedEventAttributes(started));
    History history = new History().setEvents(events);
    byte[] converted = converter.toData(history);
    History fromConverted = converter.fromData(converted, History.class, History.class);
    assertEquals(new String(converted, StandardCharsets.UTF_8), history, fromConverted);
  }

  public static void foo(List<UUID> arg) {}

  @Test
  public void testUUIDList() throws NoSuchMethodException {
    Method m = JsonDataConverterTest.class.getDeclaredMethod("foo", List.class);
    Type arg = m.getGenericParameterTypes()[0];

    List<UUID> list = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      list.add(UUID.randomUUID());
    }
    DataConverter converter = JsonDataConverter.getInstance();
    byte[] data = converter.toData(list);
    @SuppressWarnings("unchecked")
    List<UUID> result = (List<UUID>) converter.fromDataArray(data, arg)[0];
    assertEquals(result.toString(), list, result);
  }

  public static void threeArguments(int one, int two, String three) {}

  public static void aLotOfArguments(int one, int two, String three, Object obj, int[] intArr) {}

  @Test
  public void AdditionalInputArgumentsAreIgnored() throws NoSuchMethodException {
    Method m =
        JsonDataConverterTest.class.getDeclaredMethod(
            "threeArguments", int.class, int.class, String.class);
    Type[] arg = m.getGenericParameterTypes();

    DataConverter converter = JsonDataConverter.getInstance();
    byte[] data = converter.toData(1, 2, "a string", "an extra string :o!!!");
    @SuppressWarnings("unchecked")
    Object[] deserializedArguments = converter.fromDataArray(data, arg);
    assertEquals(3, deserializedArguments.length);
    assertEquals(1, (int) deserializedArguments[0]);
    assertEquals(2, (int) deserializedArguments[1]);
    assertEquals("a string", deserializedArguments[2]);
  }

  @Test
  public void MissingInputArgumentsArePopulatedWithDefaultValues() throws NoSuchMethodException {
    Method m =
        JsonDataConverterTest.class.getDeclaredMethod(
            "aLotOfArguments", int.class, int.class, String.class, Object.class, int[].class);
    Type[] arg = m.getGenericParameterTypes();

    DataConverter converter = JsonDataConverter.getInstance();
    byte[] data = converter.toData(1);
    @SuppressWarnings("unchecked")
    Object[] deserializedArguments = converter.fromDataArray(data, arg);
    assertEquals(5, deserializedArguments.length);
    assertEquals(1, (int) deserializedArguments[0]);
    assertEquals(0, (int) deserializedArguments[1]);
    assertEquals(null, deserializedArguments[2]);
    assertEquals(null, deserializedArguments[3]);
    assertEquals(null, deserializedArguments[4]);
  }

  @Test
  public void testClass() {
    DataConverter converter = JsonDataConverter.getInstance();
    byte[] data = converter.toData(this.getClass());
    @SuppressWarnings("unchecked")
    Class result = converter.fromData(data, Class.class, Class.class);
    assertEquals(result.toString(), this.getClass(), result);
  }
}
