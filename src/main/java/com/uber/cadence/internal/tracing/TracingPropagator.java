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

package com.uber.cadence.internal.tracing;

import com.uber.cadence.Header;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.WorkflowExecutionStartedEventAttributes;
import com.uber.cadence.internal.replay.DecisionContext;
import com.uber.cadence.internal.replay.ExecuteLocalActivityParameters;
import com.uber.cadence.internal.worker.LocalActivityWorker.Task;
import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopSpan;
import io.opentracing.propagation.*;
import io.opentracing.propagation.Format;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TracingPropagator {
  // span names
  private static final String EXECUTE_WORKFLOW = "cadence-ExecuteWorkflow";
  private static final String EXECUTE_ACTIVITY = "cadence-ExecuteActivity";
  private static final String EXECUTE_LOCAL_ACTIVITY = "cadence-ExecuteLocalActivity";

  // span tags
  private static final String TAG_WORKFLOW_ID = "cadenceWorkflowID";
  private static final String TAG_WORKFLOW_TYPE = "cadenceWorkflowType";
  private static final String TAG_WORKFLOW_RUN_ID = "cadenceRunID";
  private static final String TAG_ACTIVITY_TYPE = "cadenceActivityType";

  private final Tracer tracer;

  public TracingPropagator(Tracer tracer) {
    this.tracer = tracer;
  }

  public Span activateSpanByServiceMethod(String serviceMethod) {
    Span span = tracer.buildSpan(serviceMethod).asChildOf(tracer.activeSpan()).start();
    tracer.activateSpan(span);
    return span;
  }

  public Span activateSpanForExecuteWorkflow(DecisionContext context) {
    WorkflowExecutionStartedEventAttributes attributes =
        context.getWorkflowExecutionStartedEventAttributes();
    SpanContext parent = extract(attributes.getHeader());

    Span span =
        tracer
            .buildSpan(EXECUTE_WORKFLOW)
            .addReference(
                References.FOLLOWS_FROM, parent != NoopSpan.INSTANCE.context() ? parent : null)
            .withTag(TAG_WORKFLOW_TYPE, context.getWorkflowType().getName())
            .withTag(TAG_WORKFLOW_ID, context.getWorkflowId())
            .withTag(TAG_WORKFLOW_RUN_ID, context.getRunId())
            .start();
    tracer.activateSpan(span);
    return span;
  }

  public Span activateSpanForExecuteActivity(PollForActivityTaskResponse task) {
    SpanContext parent = extract(task.getHeader());
    Span span =
        tracer
            .buildSpan(EXECUTE_ACTIVITY)
            .addReference(
                References.FOLLOWS_FROM, parent != NoopSpan.INSTANCE.context() ? parent : null)
            .withTag(
                TAG_WORKFLOW_TYPE,
                task.isSetWorkflowType() ? task.getWorkflowType().getName() : "null")
            .withTag(
                TAG_WORKFLOW_ID,
                task.isSetWorkflowExecution()
                    ? task.getWorkflowExecution().getWorkflowId()
                    : "null")
            .withTag(
                TAG_WORKFLOW_RUN_ID,
                task.isSetWorkflowExecution() ? task.getWorkflowExecution().getRunId() : "null")
            .withTag(
                TAG_ACTIVITY_TYPE,
                task.isSetActivityType() ? task.getActivityType().getName() : "null")
            .start();
    tracer.activateSpan(span);
    return span;
  }

  public Span activateSpanForExecuteLocalActivity(Task task) {
    ExecuteLocalActivityParameters params = task.getExecuteLocalActivityParameters();

    // retrieve spancontext from params
    SpanContext parent = extract(params.getContext());

    Span span =
        tracer
            .buildSpan(EXECUTE_LOCAL_ACTIVITY)
            .addReference(References.FOLLOWS_FROM, parent)
            .withTag(TAG_WORKFLOW_ID, params.getWorkflowExecution().getWorkflowId())
            .withTag(TAG_WORKFLOW_RUN_ID, params.getWorkflowExecution().getRunId())
            .withTag(TAG_ACTIVITY_TYPE, params.getActivityType().getName())
            .start();
    tracer.activateSpan(span);
    return span;
  }

  public void inject(Map<String, byte[]> headers) {
    Map<String, String> context = getCurrentContext();
    context.forEach(
        (k, v) -> {
          headers.put(k, v.getBytes());
        });
  }

  public void inject(Header header) {
    Map<String, String> context = getCurrentContext();
    context.forEach(
        (k, v) -> {
          header.putToFields(k, ByteBuffer.wrap(v.getBytes()));
        });
  }

  private Map<String, String> getCurrentContext() {
    Map<String, String> context = new HashMap<>();
    if (tracer.activeSpan() != null) {
      tracer.inject(
          tracer.activeSpan().context(), Format.Builtin.TEXT_MAP, new TextMapAdapter(context));
    }
    return context;
  }

  private SpanContext extract(Map<String, byte[]> headers) {
    if (headers == null) return NoopSpan.INSTANCE.context();
    return tracer.extract(
        Format.Builtin.TEXT_MAP,
        new TextMapAdapter(
            headers
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new String(e.getValue())))));
  }

  private SpanContext extract(Header header) {
    if (header == null || header.getFields() == null) return NoopSpan.INSTANCE.context();
    return tracer.extract(
        Format.Builtin.TEXT_MAP,
        new TextMapAdapter(
            header
                .getFields()
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                          byte[] bytes = new byte[e.getValue().remaining()];
                          e.getValue().get(bytes);
                          return new String(bytes);
                        }))));
  }
}
