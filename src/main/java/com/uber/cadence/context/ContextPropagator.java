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

package com.uber.cadence.context;

import java.util.Map;

/**
 * Context Propagators are used to propagate information from workflow to activity, workflow to
 * child workflow, and workflow to child thread (using {@link com.uber.cadence.workflow.Async}).
 *
 * <p>A sample <code>ContextPropagator</code> that copies all {@link org.slf4j.MDC} entries starting
 * with a given prefix along the code path looks like this:
 *
 * <pre>{@code
 * public class MDCContextPropagator implements ContextPropagator {
 *
 *     public String getName() {
 *         return this.getClass().getName();
 *     }
 *
 *     public Object getCurrentContext() {
 *         Map<String, String> context = new HashMap<>();
 *         for (Map.Entry<String, String> entry : MDC.getCopyOfContextMap().entrySet()) {
 *             if (entry.getKey().startsWith("X-")) {
 *                 context.put(entry.getKey(), entry.getValue());
 *             }
 *         }
 *         return context;
 *     }
 *
 *     public void setCurrentContext(Object context) {
 *         Map<String, String> contextMap = (Map<String, String>)context;
 *         for (Map.Entry<String, String> entry : contextMap.entrySet()) {
 *             MDC.put(entry.getKey(), entry.getValue());
 *         }
 *     }
 *
 *     public Map<String, byte[]> serializeContext(Object context) {
 *         Map<String, String> contextMap = (Map<String, String>)context;
 *         Map<String, byte[]> serializedContext = new HashMap<>();
 *         for (Map.Entry<String, String> entry : contextMap.entrySet()) {
 *             serializedContext.put(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
 *         }
 *         return serializedContext;
 *     }
 *
 *     public Object deserializeContext(Map<String, byte[]> context) {
 *         Map<String, String> contextMap = new HashMap<>();
 *         for (Map.Entry<String, byte[]> entry : context.entrySet()) {
 *             contextMap.put(entry.getKey(), new String(entry.getValue(), StandardCharsets.UTF_8));
 *         }
 *         return contextMap;
 *     }
 * }
 * }</pre>
 *
 * To set up the context propagators, you must configure them when: <br>
 * Setting up your workflow/activity workers:
 *
 * <pre>{@code
 * Worker.FactoryOptions factoryOptions = new Worker.FactoryOptions.Builder()
 *                 .setContextPropagators(Collections.singletonList(new MDCContextPropagator()))
 *                 .build();
 * Worker.Factory factory = new Worker.Factory("cadence", 7933,"test-domain", factoryOptions);
 * }</pre>
 *
 * <br>
 * Creating your {@link com.uber.cadence.client.WorkflowClient}:
 *
 * <pre>{@code
 * WorkflowOptions options = new WorkflowOptions.Builder()
 *                 .setExecutionStartToCloseTimeout(Duration.ofSeconds(5))
 *                 .setTaskList("myTaskList")
 *                 .setContextPropagators(Collections.singletonList(new MDCContextPropagator()))
 *                 .build();
 *
 * WorkflowClient workflowClient = WorkflowClient.newInstance("cadence", 7933,"test-domain");
 * }</pre>
 *
 * <br>
 * If you want to have override the {@code ContextPropagator} instances for your activities, you can
 * specify them at the {@link com.uber.cadence.activity.ActivityOptions} level like so:
 *
 * <pre>{@code
 * activities = Workflow.newActivityStub(Activity.class,
 *                 new ActivityOptions.Builder()
 *                         .setScheduleToCloseTimeout(Duration.ofSeconds(60))
 *                         .setContextPropagators(Collections.singletonList(new MDCContextPropagator()))
 *                         .build());
 * }</pre>
 *
 * <br>
 * And similarly, if you wish to override them for child workflows, you can do so when creating a
 * {@link com.uber.cadence.workflow.ChildWorkflowStub}:
 *
 * <pre>{@code
 * ChildWorkflow childWorkflow = Workflow.newChildWorkflowStub(ChildWorkflow.class,
 *                new ChildWorkflowOptions.Builder()
 *                        .setContextPropagators(Collections.singletonList(new MDCContextPropagator()))
 *                        .build());
 * }</pre>
 */
public interface ContextPropagator {

  /**
   * Returns the name of the context propagator (for use in serialization and transfer).
   * ContextPropagators will only be given context information
   *
   * @return The name of this propagator
   */
  String getName();

  /** Given context data, serialize it for transmission in the Cadence header */
  Map<String, byte[]> serializeContext(Object context);

  /** Turn the serialized header data into context object(s) */
  Object deserializeContext(Map<String, byte[]> context);

  /** Returns the current context in object form */
  Object getCurrentContext();

  /** Sets the current context */
  void setCurrentContext(Object context);

  /** Unsets the current context. This is called when the context is no longer needed */
  void unsetCurrentContext();

  /** A context task that may throw a checked exception. */
  @FunctionalInterface
  interface ContextRunnable {
    void run() throws Exception;
  }

  /**
   * Executes {@code task} with {@code context} installed as the current context.
   *
   * <p>The default implementation preserves the legacy imperative context lifecycle. Propagators
   * that use lexical context, such as Java Scoped Values, should override this method and execute
   * {@code task} inside their lexical binding rather than implementing {@link #setCurrentContext}
   * and {@link #unsetCurrentContext}.
   *
   * <p><b>Contract:</b> implementations must let any exception thrown by {@code task} propagate to
   * the caller unchanged. Only wrap the call to {@code task.run()} in {@code try}/{@code finally}
   * for cleanup (as the default implementation does) -- never in a {@code try}/{@code catch} that
   * suppresses or replaces the exception. Cadence relies on exceptions thrown by the wrapped task
   * (including workflow cancellation and thread-destruction signals) reaching the caller in order
   * to function correctly; a propagator that swallows them causes {@link
   * com.uber.cadence.internal.context.ContextThreadLocal} to throw an internal error rather than
   * silently continue as if {@code task} had succeeded.
   */
  default void runWithContext(Object context, ContextRunnable task) throws Exception {
    setCurrentContext(context);
    try {
      task.run();
    } finally {
      unsetCurrentContext();
    }
  }
}
