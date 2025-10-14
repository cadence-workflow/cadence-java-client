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

import static org.junit.Assert.*;

import com.google.gson.JsonIOException;
import com.uber.cadence.History;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.TaskList;
import com.uber.cadence.WorkflowExecutionStartedEventAttributes;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.activity.Activity;
import com.uber.cadence.client.ApplicationFailureException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
  public void testEntity() {
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

  @Test
  public void testArray() {
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
    byte[] converted = converter.toData("abc", history);
    Object[] fromConverted = converter.fromDataArray(converted, String.class, History.class);
    assertEquals(new String(converted, StandardCharsets.UTF_8), "abc", fromConverted[0]);
    assertEquals(new String(converted, StandardCharsets.UTF_8), history, fromConverted[1]);
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

  public static class NonSerializableException extends RuntimeException {
    @SuppressWarnings("unused")
    private final InputStream file; // gson chokes on this field

    public NonSerializableException(Throwable cause) {
      super(cause);
      try {
        file = new FileInputStream(File.createTempFile("foo", "bar"));
      } catch (IOException e) {
        throw Activity.wrap(e);
      }
    }
  }

  @Test
  public void testException() {
    RuntimeException rootException = new RuntimeException("root exception");
    NonSerializableException nonSerializableCause = new NonSerializableException(rootException);
    RuntimeException e = new RuntimeException("application exception", nonSerializableCause);

    byte[] converted = converter.toData(e);
    RuntimeException fromConverted =
        converter.fromData(converted, RuntimeException.class, RuntimeException.class);
    assertEquals(RuntimeException.class, fromConverted.getClass());
    assertEquals("application exception", fromConverted.getMessage());

    Throwable causeFromConverted = fromConverted.getCause();
    assertNotNull(causeFromConverted);
    assertEquals(DataConverterException.class, causeFromConverted.getClass());
    assertNotNull(causeFromConverted.getCause());
    assertTrue(
        causeFromConverted.getCause() instanceof JsonIOException
            || causeFromConverted.getCause() instanceof StackOverflowError);

    assertNotNull(causeFromConverted.getSuppressed());
    assertEquals(1, causeFromConverted.getSuppressed().length);

    assertEquals("root exception", causeFromConverted.getSuppressed()[0].getMessage());
  }

  @Test
  public void testExceptionNotFound() {
    String convertedString =
        "{\n"
            + "  \"detailMessage\": \"application exception\",\n"
            + "  \"stackTrace\": \"com.uber.cadence.converter.JsonDataConverterTest.testExceptionNotFound(JsonDataConverterTest.java:282)\\nsun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\\nsun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\\nsun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\\njava.lang.reflect.Method.invoke(Method.java:498)\\norg.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)\\norg.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\\norg.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)\\norg.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\\norg.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)\\norg.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)\\norg.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)\\norg.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)\\norg.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)\\norg.junit.runners.ParentRunner$4.run(ParentRunner.java:331)\\norg.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)\\norg.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)\\norg.junit.runners.ParentRunner.access$100(ParentRunner.java:66)\\norg.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)\\norg.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)\\norg.junit.runners.ParentRunner.run(ParentRunner.java:413)\\norg.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93)\\norg.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40)\\norg.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:520)\\norg.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:748)\\norg.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:443)\\norg.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211)\\n\",\n"
            + "  \"suppressedExceptions\": [],\n"
            + "  \"class\": \"com.uber.cadence.converter.ExceptionNotFound\"\n"
            + "}";
    RuntimeException fromConverted =
        converter.fromData(
            convertedString.getBytes(StandardCharsets.UTF_8),
            RuntimeException.class,
            RuntimeException.class);
    assertEquals(ApplicationFailureException.class, fromConverted.getClass());
    assertEquals("application exception", fromConverted.getMessage());
    assertNotSame(fromConverted.getStackTrace().length, 0);
  }
}
