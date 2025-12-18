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

import static org.junit.Assert.*;

import com.uber.cadence.DomainAlreadyExistsError;
import com.uber.cadence.RegisterDomainRequest;
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.client.*;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.internal.compatibility.Thrift2ProtoAdapter;
import com.uber.cadence.internal.compatibility.proto.serviceclient.IGrpcServiceStubs;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import com.uber.cadence.testUtils.TestEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.worker.WorkerFactory;
import com.uber.cadence.worker.WorkerFactoryOptions;
import com.uber.cadence.worker.WorkerOptions;
import com.uber.cadence.workflow.Async;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartWorkflowTest {
  private static final String CONTEXT_KEY = "random-context-value";
  private static final String CONTEXT_VALUE = "this should propagate";

  public interface TestWorkflow {
    @WorkflowMethod(executionStartToCloseTimeoutSeconds = 500, taskList = TASK_LIST)
    Integer AddOneThenDouble(Integer n);
  }

  public interface DoubleWorkflow {
    @WorkflowMethod(executionStartToCloseTimeoutSeconds = 500, taskList = TASK_LIST)
    Integer Double(Integer n);
  }

  public interface TestActivity {
    @ActivityMethod(scheduleToCloseTimeoutSeconds = 5)
    Integer AddOne(Integer i);

    @ActivityMethod(scheduleToCloseTimeoutSeconds = 5)
    Integer Double(Integer i);
  }

  public static class TestActivityImpl implements TestActivity {
    private final Tracer tracer;
    private final boolean shouldPropagate;

    TestActivityImpl(Tracer tracer, boolean shouldPropagate) {
      this.tracer = tracer;
      this.shouldPropagate = shouldPropagate;
    }

    @Override
    public Integer AddOne(Integer i) {
      if (shouldPropagate) {
        assertEquals(
            "context should propagate",
            CONTEXT_VALUE,
            tracer.activeSpan().getBaggageItem(CONTEXT_KEY));
      } else {
        assertNull("context should not propagate", tracer.activeSpan());
      }
      return i + 1;
    }

    @Override
    public Integer Double(Integer i) {
      if (shouldPropagate) {
        assertEquals(
            "context should propagate",
            CONTEXT_VALUE,
            tracer.activeSpan().getBaggageItem(CONTEXT_KEY));
      } else {
        assertNull("context should not propagate", tracer.activeSpan());
      }
      return i * 2;
    }
  }

  public static class TestWorkflowImpl implements TestWorkflow {
    private final TestActivity activities =
        Workflow.newActivityStub(
            TestActivity.class,
            new ActivityOptions.Builder()
                .setRetryOptions(
                    new RetryOptions.Builder()
                        .setInitialInterval(Duration.ofSeconds(10))
                        .setMaximumAttempts(2)
                        .build())
                .build());

    @Override
    public Integer AddOneThenDouble(Integer n) {
      Integer addOneRes = activities.AddOne(n);
      DoubleWorkflow workflow = Workflow.newChildWorkflowStub(DoubleWorkflow.class);
      return Async.function(workflow::Double, addOneRes).get();
    }
  }

  public static class DoubleWorkflowImpl implements DoubleWorkflow {
    private final TestActivity activities = Workflow.newLocalActivityStub(TestActivity.class);

    @Override
    public Integer Double(Integer n) {
      return activities.Double(n);
    }
  }

  private static final boolean useDockerService = TestEnvironment.isUseDockerService();
  private static final Logger logger = LoggerFactory.getLogger(StartWorkflowTest.class);
  private static final String DOMAIN = "test-domain";
  private static final String TASK_LIST = "test-tasklist";

  @Test
  public void testStartWorkflowTchannel() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new WorkflowServiceTChannel(ClientOptions.newBuilder().setTracer(mockTracer).build());
    testStartWorkflowHelper(service, mockTracer, true);
  }

  @Test
  public void testStartWorkflowGRPC() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new Thrift2ProtoAdapter(
            IGrpcServiceStubs.newInstance(
                ClientOptions.newBuilder().setTracer(mockTracer).setPort(7833).build()));
    testStartWorkflowHelper(service, mockTracer, true);
  }

  @Test
  public void testStartMultipleWorkflowGRPC() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new Thrift2ProtoAdapter(
            IGrpcServiceStubs.newInstance(
                ClientOptions.newBuilder().setTracer(mockTracer).setPort(7833).build()));
    try {
      service.RegisterDomain(new RegisterDomainRequest().setName(DOMAIN));
    } catch (DomainAlreadyExistsError e) {
      logger.info("domain already registered");
    } catch (Exception e) {
      fail("fail to register domain: " + e);
    }

    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setDomain(DOMAIN).build());

    WorkerFactory workerFactory =
        WorkerFactory.newInstance(
            client, WorkerFactoryOptions.newBuilder().setMaxWorkflowThreadCount(2).build());
    Worker worker;
    worker =
        workerFactory.newWorker(
            TASK_LIST, WorkerOptions.newBuilder().setMaxConcurrentWorkflowExecutionSize(2).build());
    worker.registerActivitiesImplementations(new TestActivityImpl(mockTracer, true));
    worker.registerWorkflowImplementationTypes(TestWorkflowImpl.class, DoubleWorkflowImpl.class);
    workerFactory.start();

    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      int finalI = i;
      futures.add(
          CompletableFuture.runAsync(
              () -> {
                Span rootSpan = mockTracer.buildSpan("workflow=" + finalI).start();
                rootSpan.setBaggageItem(CONTEXT_KEY, CONTEXT_VALUE);
                mockTracer.activateSpan(rootSpan);
                client.newWorkflowStub(TestWorkflow.class).AddOneThenDouble(finalI);
                rootSpan.finish();
              }));
    }
    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
    } catch (Exception e) {
      fail("workflow failure: " + e);
    } finally {
      // test debug log
      StringBuilder sb = new StringBuilder();

      List<MockSpan> spans = mockTracer.finishedSpans();
      spans.forEach(
          span -> {
            sb.append(span.toString()).append("\n");
          });
      logger.info("spans: " + sb);
      workerFactory.shutdown();

      // assert activity span should have only 1 parent
      List<MockSpan> filtered =
          spans
              .stream()
              .filter(
                  s ->
                      s.operationName().contains("ExecuteActivity")
                          || s.operationName().contains("ExecuteLocalActivity")
                          || s.operationName().contains("ExecuteWorkflow"))
              .collect(Collectors.toList());
      assertFalse(filtered.isEmpty());
      filtered.forEach(
          s -> {
            assertEquals(1, s.references().size());
          });
    }
  }

  @Test
  public void testSignalWithStartWorkflowTchannel() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new WorkflowServiceTChannel(ClientOptions.newBuilder().setTracer(mockTracer).build());
    testSignalWithStartWorkflowHelper(service, mockTracer, true);
  }

  @Test
  public void testSignalWithStartWorkflowGRPC() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new Thrift2ProtoAdapter(
            IGrpcServiceStubs.newInstance(
                ClientOptions.newBuilder().setTracer(mockTracer).setPort(7833).build()));
    testSignalWithStartWorkflowHelper(service, mockTracer, true);
  }

  @Test
  public void testStartWorkflowTchannelNoPropagation() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new WorkflowServiceTChannel(ClientOptions.newBuilder().setTracer(null).build());
    testStartWorkflowHelper(service, mockTracer, false);
  }

  @Test
  public void testStartWorkflowGRPCNoPropagation() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new Thrift2ProtoAdapter(
            IGrpcServiceStubs.newInstance(
                ClientOptions.newBuilder().setPort(7833).setTracer(null).build()));
    testStartWorkflowHelper(service, mockTracer, false);
  }

  @Test
  public void testSignalStartWorkflowTchannelNoPropagation() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new WorkflowServiceTChannel(ClientOptions.newBuilder().setTracer(null).build());
    testSignalWithStartWorkflowHelper(service, mockTracer, false);
  }

  @Test
  public void testSignalStartWorkflowGRPCNoPropagation() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new Thrift2ProtoAdapter(
            IGrpcServiceStubs.newInstance(
                ClientOptions.newBuilder().setPort(7833).setTracer(null).build()));
    testSignalWithStartWorkflowHelper(service, mockTracer, false);
  }

  private void testStartWorkflowHelper(
      IWorkflowService service, MockTracer mockTracer, boolean shouldPropagate) {
    try {
      service.RegisterDomain(new RegisterDomainRequest().setName(DOMAIN));
    } catch (DomainAlreadyExistsError e) {
      logger.info("domain already registered");
    } catch (Exception e) {
      fail("fail to register domain: " + e);
    }

    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setDomain(DOMAIN).build());

    WorkerFactory workerFactory =
        WorkerFactory.newInstance(client, WorkerFactoryOptions.newBuilder().build());
    Worker worker;
    worker = workerFactory.newWorker(TASK_LIST, WorkerOptions.newBuilder().build());
    worker.registerActivitiesImplementations(new TestActivityImpl(mockTracer, shouldPropagate));
    worker.registerWorkflowImplementationTypes(TestWorkflowImpl.class, DoubleWorkflowImpl.class);
    workerFactory.start();

    // start a workflow
    Span rootSpan = mockTracer.buildSpan("Test Started").start();
    rootSpan.setBaggageItem(CONTEXT_KEY, CONTEXT_VALUE);
    mockTracer.activateSpan(rootSpan);
    try {
      TestWorkflow wf = client.newWorkflowStub(TestWorkflow.class);
      int res = wf.AddOneThenDouble(3);
      assertEquals(8, res);
    } catch (Exception e) {
      throw new AssertionError("workflow failure", e);
    } finally {
      rootSpan.finish();
      List<MockSpan> spans = mockTracer.finishedSpans();
      spans.sort(
          (o1, o2) -> {
            if (o1.startMicros() < o2.startMicros()) {
              return -1;
            } else if (o1.startMicros() > o2.startMicros()) {
              return 1;
            }
            return 0;
          });

      // test debug log
      StringBuilder sb = new StringBuilder();
      spans.forEach(
          span -> {
            sb.append(span.toString()).append("\n");
          });
      logger.info("spans: " + sb);

      if (!shouldPropagate) {
        assertEquals(1, spans.size());
        workerFactory.shutdown();
      } else {
        // assert start workflow
        MockSpan spanStartWorkflow =
            getFirstSpanByOperationName(spans, "cadence-StartWorkflowExecution");

        // assert workflow spans
        MockSpan spanExecuteWF = getFirstSpanByOperationName(spans, "cadence-ExecuteWorkflow");
        assertChildOf(spans, spanExecuteWF, spanStartWorkflow);

        MockSpan spanExecuteActivity = getLinkedSpans(spans, spanExecuteWF.context()).get(0);
        assertEquals(spanExecuteActivity.operationName(), "cadence-ExecuteActivity");
        assertSpanReferences(spanExecuteActivity, "follows_from", spanExecuteWF);

        MockSpan spanExecuteChildWF = getLinkedSpans(spans, spanExecuteWF.context()).get(1);
        assertEquals(spanExecuteChildWF.operationName(), "cadence-ExecuteWorkflow");
        assertSpanReferences(spanExecuteChildWF, "follows_from", spanExecuteWF);

        MockSpan spanExecuteLocalActivity =
            getLinkedSpans(spans, spanExecuteChildWF.context()).get(0);
        assertEquals(spanExecuteLocalActivity.operationName(), "cadence-ExecuteLocalActivity");
        assertSpanReferences(spanExecuteLocalActivity, "follows_from", spanExecuteChildWF);
      }
      workerFactory.shutdown();
    }
  }

  private void testSignalWithStartWorkflowHelper(
      IWorkflowService service, MockTracer mockTracer, boolean shouldPropagate) {
    try {
      service.RegisterDomain(new RegisterDomainRequest().setName(DOMAIN));
    } catch (DomainAlreadyExistsError e) {
      logger.info("domain already registered");
    } catch (Exception e) {
      fail("fail to register domain: " + e);
    }

    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setDomain(DOMAIN).build());

    WorkerFactory workerFactory =
        WorkerFactory.newInstance(client, WorkerFactoryOptions.newBuilder().build());
    Worker worker;
    worker = workerFactory.newWorker(TASK_LIST, WorkerOptions.newBuilder().build());
    worker.registerActivitiesImplementations(new TestActivityImpl(mockTracer, shouldPropagate));
    worker.registerWorkflowImplementationTypes(TestWorkflowImpl.class, DoubleWorkflowImpl.class);
    workerFactory.start();

    // start a workflow
    Span rootSpan = mockTracer.buildSpan("Test Started").start();
    rootSpan.setBaggageItem(CONTEXT_KEY, CONTEXT_VALUE);
    mockTracer.activateSpan(rootSpan);
    try {
      WorkflowStub wf =
          client.newUntypedWorkflowStub(
              "TestWorkflow::AddOneThenDouble",
              new WorkflowOptions.Builder()
                  .setExecutionStartToCloseTimeout(Duration.ofSeconds(60))
                  .setTaskList(TASK_LIST)
                  .build());
      wf.signalWithStart(
          "start workflow",
          new Object[] {},
          new Object[] {
            3,
          });
      int res = wf.getResult(Integer.class);
      assertEquals(8, res);
    } catch (Exception e) {
      throw new AssertionError("Workflow failure", e);
    } finally {
      rootSpan.finish();
      List<MockSpan> spans = mockTracer.finishedSpans();
      spans.sort(
          (o1, o2) -> {
            if (o1.startMicros() < o2.startMicros()) {
              return -1;
            } else if (o1.startMicros() > o2.startMicros()) {
              return 1;
            }
            return 0;
          });

      // test debug log
      StringBuilder sb = new StringBuilder();
      spans.forEach(
          span -> {
            sb.append(span.toString()).append("\n");
          });
      logger.info("spans: " + sb);

      if (!shouldPropagate) {
        assertEquals(1, spans.size());
        workerFactory.shutdown();
      } else {
        // assert start workflow
        MockSpan spanStartWorkflow =
            getFirstSpanByOperationName(spans, "cadence-SignalWithStartWorkflowExecution");

        // assert workflow spans
        List<MockSpan> workflowSpans =
            getSpansByTraceID(spans, spanStartWorkflow.context().toTraceId());
        MockSpan spanExecuteWF =
            getFirstSpanByOperationName(workflowSpans, "cadence-ExecuteWorkflow");
        assertChildOf(spans, spanExecuteWF, spanStartWorkflow);

        MockSpan spanExecuteActivity = getLinkedSpans(spans, spanExecuteWF.context()).get(0);
        assertEquals(spanExecuteActivity.operationName(), "cadence-ExecuteActivity");
        assertSpanReferences(spanExecuteActivity, "follows_from", spanExecuteWF);

        MockSpan spanExecuteChildWF = getLinkedSpans(spans, spanExecuteWF.context()).get(1);
        assertEquals(spanExecuteChildWF.operationName(), "cadence-ExecuteWorkflow");
        assertSpanReferences(spanExecuteChildWF, "follows_from", spanExecuteWF);

        MockSpan spanExecuteLocalActivity =
            getLinkedSpans(spans, spanExecuteChildWF.context()).get(0);
        assertEquals(spanExecuteLocalActivity.operationName(), "cadence-ExecuteLocalActivity");
        assertSpanReferences(spanExecuteLocalActivity, "follows_from", spanExecuteChildWF);
      }
      workerFactory.shutdown();
    }
  }

  private MockSpan getFirstSpanByOperationName(List<MockSpan> spans, String operation) {
    return spans
        .stream()
        .filter(span -> span.operationName().equals(operation))
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Failed to find span with operation: " + operation));
  }

  private List<MockSpan> getSpansByTraceID(List<MockSpan> spans, String traceID) {
    return spans
        .stream()
        .filter(span -> span.context().toTraceId().equals(traceID))
        .collect(Collectors.toList());
  }

  private List<MockSpan> getLinkedSpans(List<MockSpan> spans, SpanContext spanContext) {
    return spans
        .stream()
        .filter(
            span ->
                span.references()
                    .stream()
                    .anyMatch(
                        reference ->
                            reference.getContext().toSpanId().equals(spanContext.toSpanId())
                                && reference
                                    .getContext()
                                    .toTraceId()
                                    .equals(spanContext.toTraceId())))
        .collect(Collectors.toList());
  }

  void assertChildOf(List<MockSpan> spans, MockSpan span, MockSpan parentSpan) {
    Map<String, MockSpan> byId = new HashMap<>();
    for (MockSpan s : spans) {
      byId.put(s.context().traceId() + "-" + s.context().toSpanId(), s);
    }
    Queue<MockSpan> toVisit = new LinkedList<>();
    toVisit.add(span);
    while (!toVisit.isEmpty()) {
      MockSpan current = toVisit.poll();
      MockSpan parent = byId.get(current.context().traceId() + "-" + current.parentId());
      if (parent != null) {
        if (parent.equals(parentSpan)) {
          return;
        } else {
          toVisit.add(parent);
        }
      }
    }
    fail(String.format("span %s is not a child of parent span %s", span, parentSpan));
  }

  void assertSpanReferences(MockSpan span, String referenceType, MockSpan parentSpan) {
    assertTrue(
        String.format(
            "span %s has reference %s to parent span %s: %s",
            span, referenceType, parentSpan, span.references()),
        span.references()
            .stream()
            .anyMatch(
                ref ->
                    Objects.equals(ref.getReferenceType(), referenceType)
                        && Objects.equals(
                            ref.getContext().toSpanId(), parentSpan.context().toSpanId())
                        && Objects.equals(
                            ref.getContext().toTraceId(), parentSpan.context().toTraceId())));
  }
}
