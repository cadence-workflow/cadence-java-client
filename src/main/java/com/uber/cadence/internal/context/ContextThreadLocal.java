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

package com.uber.cadence.internal.context;

import com.uber.cadence.context.ContextPropagator;
import com.uber.cadence.context.ContextPropagator.ContextRunnable;
import com.uber.cadence.workflow.WorkflowThreadLocal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** This class holds the current set of context propagators */
public class ContextThreadLocal {

  private static WorkflowThreadLocal<List<ContextPropagator>> contextPropagators =
      WorkflowThreadLocal.withInitial(
          new Supplier<List<ContextPropagator>>() {
            @Override
            public List<ContextPropagator> get() {
              return new ArrayList<>();
            }
          });

  /** Sets the list of context propagators for the thread */
  public static void setContextPropagators(List<ContextPropagator> propagators) {
    if (propagators == null || propagators.isEmpty()) {
      return;
    }
    contextPropagators.set(propagators);
  }

  public static List<ContextPropagator> getContextPropagators() {
    return contextPropagators.get();
  }

  public static Map<String, Object> getCurrentContextForPropagation() {
    Map<String, Object> contextData = new HashMap<>();
    for (ContextPropagator propagator : contextPropagators.get()) {
      contextData.put(propagator.getName(), propagator.getCurrentContext());
    }
    return contextData;
  }

  public static void runWithContext(Map<String, Object> contextData, ContextRunnable task)
      throws Exception {
    runWithContext(contextPropagators.get(), contextData, task);
  }

  /**
   * Executes {@code task} inside every applicable propagator context.
   *
   * <p>Propagators are composed in configuration order, so the first propagator is the outermost
   * context and cleanup occurs in reverse order. Legacy propagators retain their existing set/unset
   * behavior through {@link ContextPropagator#runWithContext(Object, ContextRunnable)}.
   *
   * <p>Every {@link ContextPropagator#runWithContext(Object, ContextRunnable)} implementation in
   * the chain is required to call {@code task.run()} exactly once and propagate any exception it
   * throws, rather than skipping the call, retrying it, or catching and suppressing the exception.
   * This method verifies that contract: if {@code task} is invoked a number of times other than
   * exactly one, or if it throws but no exception escapes the composed propagator chain, a {@link
   * ContextPropagatorContractViolationError} is thrown instead of silently continuing as if {@code
   * task} had succeeded normally.
   */
  public static void runWithContext(
      List<ContextPropagator> propagators, Map<String, Object> contextData, ContextRunnable task)
      throws Exception {
    if (propagators == null
        || propagators.isEmpty()
        || contextData == null
        || contextData.isEmpty()) {
      task.run();
      return;
    }

    List<ContextPropagator> applied = new ArrayList<>();
    AtomicInteger invocationCount = new AtomicInteger();
    AtomicReference<Throwable> thrown = new AtomicReference<>();
    ContextRunnable invocation =
        () -> {
          invocationCount.incrementAndGet();
          try {
            task.run();
          } catch (Throwable t) {
            thrown.set(t);
            throw t;
          }
        };
    for (int i = propagators.size() - 1; i >= 0; i--) {
      ContextPropagator propagator = propagators.get(i);
      if (contextData.containsKey(propagator.getName())) {
        applied.add(0, propagator);
        Object context = contextData.get(propagator.getName());
        ContextRunnable next = invocation;
        invocation = () -> propagator.runWithContext(context, next);
      }
    }
    invocation.run();

    if (invocationCount.get() != 1) {
      throw ContextPropagatorContractViolationError.unexpectedInvocationCount(
          applied, invocationCount.get(), thrown.get());
    }
    Throwable swallowed = thrown.get();
    if (swallowed != null) {
      throw ContextPropagatorContractViolationError.swallowedException(applied, swallowed);
    }
  }
}
