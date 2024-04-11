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
import com.uber.cadence.client.*;
import com.uber.cadence.internal.compatibility.Thrift2ProtoAdapter;
import com.uber.cadence.internal.compatibility.proto.serviceclient.IGrpcServiceStubs;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
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
import java.util.List;
import java.util.Objects;
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

    TestActivityImpl(Tracer tracer) {
      this.tracer = tracer;
    }

    @Override
    public Integer AddOne(Integer i) {
      assertEquals(
          "context should propagate",
          CONTEXT_VALUE,
          tracer.activeSpan().getBaggageItem(CONTEXT_KEY));
      return i + 1;
    }

    @Override
    public Integer Double(Integer i) {
      assertEquals(
          "context should propagate",
          CONTEXT_VALUE,
          tracer.activeSpan().getBaggageItem(CONTEXT_KEY));
      return i * 2;
    }
  }

  public static class TestWorkflowImpl implements TestWorkflow {
    private final TestActivity activities = Workflow.newActivityStub(TestActivity.class);

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

  private static final boolean useDockerService =
      Boolean.parseBoolean(System.getenv("USE_DOCKER_SERVICE"));
  private static final Logger logger = LoggerFactory.getLogger(StartWorkflowTest.class);
  private static final String DOMAIN = "test-domain";
  private static final String TASK_LIST = "test-tasklist";

  @Test
  public void testStartWorkflowTchannel() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new WorkflowServiceTChannel(ClientOptions.newBuilder().setTracer(mockTracer).build());
    testStartWorkflowHelper(service, mockTracer);
  }

  @Test
  public void testStartWorkflowGRPC() {
    //    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new Thrift2ProtoAdapter(
            IGrpcServiceStubs.newInstance(
                ClientOptions.newBuilder().setTracer(mockTracer).setPort(7833).build()));
    testStartWorkflowHelper(service, mockTracer);
  }

  @Test
  public void testSignalWithStartWorkflowTchannel() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new WorkflowServiceTChannel(ClientOptions.newBuilder().setTracer(mockTracer).build());
    testSignalWithStartWorkflowHelper(service, mockTracer);
  }

  @Test
  public void testSignalWithStartWorkflowGRPC() {
    Assume.assumeTrue(useDockerService);
    MockTracer mockTracer = new MockTracer();
    IWorkflowService service =
        new Thrift2ProtoAdapter(
            IGrpcServiceStubs.newInstance(
                ClientOptions.newBuilder().setTracer(mockTracer).setPort(7833).build()));
    testSignalWithStartWorkflowHelper(service, mockTracer);
  }

  private void testStartWorkflowHelper(IWorkflowService service, MockTracer mockTracer) {
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
    Worker worker =
        workerFactory.newWorker(
            TASK_LIST, WorkerOptions.newBuilder().setTracer(mockTracer).build());
    worker.registerActivitiesImplementations(new TestActivityImpl(mockTracer));
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
      fail("workflow failure: " + e);
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

      // assert start workflow
      MockSpan spanStartWorkflow =
          spans
              .stream()
              .filter(span -> span.operationName().contains("StartWorkflow"))
              .findFirst()
              .orElse(null);
      if (spanStartWorkflow == null) {
        fail("StartWorkflow span not found");
      }

      // assert workflow spans
      MockSpan spanExecuteWF = getLinkedSpans(spans, spanStartWorkflow.context()).get(1);
      assertEquals(spanExecuteWF.operationName(), "cadence-ExecuteWorkflow");
      assertSpanReferences(spanExecuteWF, "follows_from", spanStartWorkflow);

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

      workerFactory.shutdown();
    }
  }

  private void testSignalWithStartWorkflowHelper(IWorkflowService service, MockTracer mockTracer) {
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
    Worker worker =
        workerFactory.newWorker(
            TASK_LIST, WorkerOptions.newBuilder().setTracer(mockTracer).build());
    worker.registerActivitiesImplementations(new TestActivityImpl(mockTracer));
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
      fail("workflow failure: " + e);
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

      // assert start workflow
      MockSpan spanStartWorkflow =
          spans
              .stream()
              .filter(span -> span.operationName().contains("StartWorkflow"))
              .findFirst()
              .orElse(null);
      if (spanStartWorkflow == null) {
        fail("StartWorkflow span not found");
      }

      // assert workflow spans
      List<MockSpan> workflowSpans =
          getSpansByTraceID(spans, spanStartWorkflow.context().toTraceId());
      MockSpan spanExecuteWF = getLinkedSpans(spans, spanStartWorkflow.context()).get(1);
      assertEquals(spanExecuteWF.operationName(), "cadence-ExecuteWorkflow");
      assertSpanReferences(spanExecuteWF, "follows_from", spanStartWorkflow);

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

      workerFactory.shutdown();
    }
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
