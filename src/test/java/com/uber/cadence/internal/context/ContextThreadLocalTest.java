/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
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

package com.uber.cadence.internal.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.uber.cadence.context.ContextPropagator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class ContextThreadLocalTest {

  @Test
  public void runWithContextNestsLegacyPropagatorsAndRestoresThemInReverseOrder() throws Exception {
    List<String> events = new ArrayList<>();
    ContextPropagator outer = new RecordingPropagator("outer", events);
    ContextPropagator inner = new RecordingPropagator("inner", events);

    ContextThreadLocal.runWithContext(
        Arrays.asList(outer, inner),
        context("outer", "outer-context", "inner", "inner-context"),
        () -> {
          events.add("task");
        });

    assertEquals(
        Arrays.asList(
            "outer:set:outer-context",
            "inner:set:inner-context",
            "task",
            "inner:unset",
            "outer:unset"),
        events);
  }

  @Test
  public void runWithContextUsesLexicalPropagatorOverride() throws Exception {
    List<String> events = new ArrayList<>();
    ContextPropagator scoped = new ScopedRecordingPropagator("scoped", events);

    ContextThreadLocal.runWithContext(
        Collections.singletonList(scoped),
        Collections.<String, Object>singletonMap("scoped", "scoped-context"),
        () -> {
          events.add("task");
        });

    assertEquals(Arrays.asList("scoped:scope:scoped-context", "task", "scoped:close"), events);
  }

  @Test
  public void runWithContextRestoresLegacyPropagatorAfterTaskFailure() {
    List<String> events = new ArrayList<>();
    ContextPropagator propagator = new RecordingPropagator("context", events);

    try {
      ContextThreadLocal.runWithContext(
          Collections.singletonList(propagator),
          Collections.<String, Object>singletonMap("context", "value"),
          () -> {
            events.add("task");
            throw new IllegalStateException("failure");
          });
      fail("expected task failure");
    } catch (Exception e) {
      assertEquals("failure", e.getMessage());
    }

    assertEquals(Arrays.asList("context:set:value", "task", "context:unset"), events);
  }

  @Test
  public void runWithContextThrowsWhenPropagatorSwallowsTaskException() {
    List<String> events = new ArrayList<>();
    ContextPropagator swallowing = new SwallowingPropagator("swallowing", events);
    IllegalStateException taskFailure = new IllegalStateException("failure");

    try {
      ContextThreadLocal.runWithContext(
          Collections.singletonList(swallowing),
          Collections.<String, Object>singletonMap("swallowing", "value"),
          () -> {
            events.add("task");
            throw taskFailure;
          });
      fail("expected ContextPropagatorSwallowedExceptionError");
    } catch (ContextPropagatorSwallowedExceptionError e) {
      assertEquals(taskFailure, e.getCause());
    } catch (Exception e) {
      fail("expected ContextPropagatorSwallowedExceptionError, got " + e);
    }

    assertEquals(Arrays.asList("task", "swallowing:caught"), events);
  }

  private static Map<String, Object> context(
      final String firstName,
      final Object firstValue,
      final String secondName,
      final Object secondValue) {
    Map<String, Object> context = new java.util.HashMap<>();
    context.put(firstName, firstValue);
    context.put(secondName, secondValue);
    return context;
  }

  private static class RecordingPropagator implements ContextPropagator {
    protected final String name;
    protected final List<String> events;

    RecordingPropagator(final String name, final List<String> events) {
      this.name = name;
      this.events = events;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Map<String, byte[]> serializeContext(final Object context) {
      return Collections.emptyMap();
    }

    @Override
    public Object deserializeContext(final Map<String, byte[]> context) {
      return null;
    }

    @Override
    public Object getCurrentContext() {
      return null;
    }

    @Override
    public void setCurrentContext(final Object context) {
      events.add(name + ":set:" + context);
    }

    @Override
    public void unsetCurrentContext() {
      events.add(name + ":unset");
    }
  }

  private static final class ScopedRecordingPropagator extends RecordingPropagator {
    ScopedRecordingPropagator(final String name, final List<String> events) {
      super(name, events);
    }

    @Override
    public void runWithContext(final Object context, final ContextRunnable task) throws Exception {
      events.add(name + ":scope:" + context);
      try {
        task.run();
      } finally {
        events.add(name + ":close");
      }
    }
  }

  private static final class SwallowingPropagator extends RecordingPropagator {
    SwallowingPropagator(final String name, final List<String> events) {
      super(name, events);
    }

    @Override
    public void runWithContext(final Object context, final ContextRunnable task) throws Exception {
      try {
        task.run();
      } catch (Exception e) {
        // Deliberately violates the ContextPropagator#runWithContext contract for testing.
        events.add(name + ":caught");
      }
    }
  }
}
