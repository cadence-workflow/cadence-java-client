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

package com.uber.cadence.internal.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.uber.cadence.FakeWorkflowServiceRule;
import com.uber.cadence.ServiceBusyError;
import com.uber.cadence.SignalWithStartWorkflowExecutionAsyncResponse;
import com.uber.cadence.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionAsyncRequest;
import com.uber.cadence.StartWorkflowExecutionAsyncResponse;
import com.uber.cadence.StartWorkflowExecutionResponse;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.client.BatchRequest;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.client.WorkflowStub;
import com.uber.cadence.workflow.SignalMethod;
import com.uber.cadence.workflow.WorkflowMethod;
import io.opentracing.mock.MockSpan;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.io.Charsets;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class WorkflowClientInternalTest {

  @ClassRule public static FakeWorkflowServiceRule fakeService = new FakeWorkflowServiceRule();

  private WorkflowClient client;

  @Before
  public void setup() throws Exception {
    fakeService.resetStubs();
    client =
        WorkflowClient.newInstance(
            fakeService.getClient(),
            WorkflowClientOptions.newBuilder().setDomain("domain").build());
  }

  @Test
  public void testEnqueueStart() throws Exception {
    CompletableFuture<WorkflowService.StartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubSuccess(
            "WorkflowService::StartWorkflowExecutionAsync",
            WorkflowService.StartWorkflowExecutionAsync_args.class,
            new WorkflowService.StartWorkflowExecutionAsync_result()
                .setSuccess(new StartWorkflowExecutionAsyncResponse()));

    WorkflowStub stub =
        client.newUntypedWorkflowStub(
            "type",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());
    stub.enqueueStart("input");

    StartWorkflowExecutionAsyncRequest request = requestFuture.getNow(null).getStartRequest();
    assertEquals(new WorkflowType().setName("type"), request.getRequest().getWorkflowType());
    assertEquals("workflowId", request.getRequest().getWorkflowId());
    assertEquals(1, request.getRequest().getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, request.getRequest().getTaskStartToCloseTimeoutSeconds());
    assertEquals("domain", request.getRequest().getDomain());
    assertEquals("taskList", request.getRequest().getTaskList().getName());
    assertEquals("\"input\"", StandardCharsets.UTF_8.decode(request.request.input).toString());
    assertNotNull(request.getRequest().getRequestId());
  }

  @Test
  public void testEnqueueStart_includesTracing() {
    CompletableFuture<WorkflowService.StartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubSuccess(
            "WorkflowService::StartWorkflowExecutionAsync",
            WorkflowService.StartWorkflowExecutionAsync_args.class,
            new WorkflowService.StartWorkflowExecutionAsync_result()
                .setSuccess(new StartWorkflowExecutionAsyncResponse()));

    WorkflowStub stub =
        client.newUntypedWorkflowStub(
            "type",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());
    stub.enqueueStart("input");

    StartWorkflowExecutionAsyncRequest request = requestFuture.getNow(null).getStartRequest();
    MockSpan mockSpan =
        fakeService
            .getTracer()
            .finishedSpans()
            .stream()
            .filter(span -> "cadence-StartWorkflowExecutionAsync".equals(span.operationName()))
            .findFirst()
            .orElseThrow(
                () ->
                    new AssertionError(
                        "No span found for StartWorkflowExecutionAsync:"
                            + fakeService.getTracer().finishedSpans()));
    assertEquals(
        mockSpan.context().toTraceId(),
        Charsets.UTF_8
            .decode(request.getRequest().getHeader().getFields().get("traceid"))
            .toString());
    assertEquals(
        mockSpan.context().toSpanId(),
        Charsets.UTF_8
            .decode(request.getRequest().getHeader().getFields().get("spanid"))
            .toString());
  }

  interface TestWorkflow {
    @WorkflowMethod(
      taskList = "taskList",
      executionStartToCloseTimeoutSeconds = 1,
      taskStartToCloseTimeoutSeconds = 2
    )
    void test(String input);
  }

  @Test
  public void testEnqueueStart_stronglyTyped() throws Exception {
    CompletableFuture<WorkflowService.StartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubSuccess(
            "WorkflowService::StartWorkflowExecutionAsync",
            WorkflowService.StartWorkflowExecutionAsync_args.class,
            new WorkflowService.StartWorkflowExecutionAsync_result()
                .setSuccess(new StartWorkflowExecutionAsyncResponse()));

    TestWorkflow stub =
        client.newWorkflowStub(
            TestWorkflow.class,
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());

    WorkflowExecution execution = WorkflowClient.enqueueStart(stub::test, "input");

    assertEquals(new WorkflowExecution().setWorkflowId("workflowId"), execution);
    StartWorkflowExecutionAsyncRequest request = requestFuture.getNow(null).getStartRequest();
    assertEquals(
        new WorkflowType().setName("TestWorkflow::test"), request.getRequest().getWorkflowType());
    assertEquals("workflowId", request.getRequest().getWorkflowId());
    assertEquals(1, request.getRequest().getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, request.getRequest().getTaskStartToCloseTimeoutSeconds());
    assertEquals("domain", request.getRequest().getDomain());
    assertEquals("taskList", request.getRequest().getTaskList().getName());
    assertEquals("\"input\"", StandardCharsets.UTF_8.decode(request.request.input).toString());
    assertNotNull(request.getRequest().getRequestId());
  }

  @Test
  public void testEnqueueStartAsync() {
    CompletableFuture<WorkflowService.StartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubSuccess(
            "WorkflowService::StartWorkflowExecutionAsync",
            WorkflowService.StartWorkflowExecutionAsync_args.class,
            new WorkflowService.StartWorkflowExecutionAsync_result()
                .setSuccess(new StartWorkflowExecutionAsyncResponse()));

    WorkflowStub stub =
        client.newUntypedWorkflowStub(
            "type",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());
    stub.enqueueStartAsync("input").join();

    StartWorkflowExecutionAsyncRequest request = requestFuture.getNow(null).getStartRequest();
    assertEquals(new WorkflowType().setName("type"), request.getRequest().getWorkflowType());
    assertEquals("workflowId", request.getRequest().getWorkflowId());
    assertEquals(1, request.getRequest().getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, request.getRequest().getTaskStartToCloseTimeoutSeconds());
    assertEquals("domain", request.getRequest().getDomain());
    assertEquals("taskList", request.getRequest().getTaskList().getName());
    assertEquals("\"input\"", StandardCharsets.UTF_8.decode(request.request.input).toString());
    assertNotNull(request.getRequest().getRequestId());
  }

  @Test
  public void testEnqueueSignalWithStart() {
    CompletableFuture<WorkflowService.SignalWithStartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubSuccess(
            "WorkflowService::SignalWithStartWorkflowExecutionAsync",
            WorkflowService.SignalWithStartWorkflowExecutionAsync_args.class,
            new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                .setSuccess(new SignalWithStartWorkflowExecutionAsyncResponse()));

    WorkflowStub stub =
        client.newUntypedWorkflowStub(
            "type",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());
    WorkflowExecution execution =
        stub.enqueueSignalWithStart(
            "signalName", new Object[] {"signalValue"}, new Object[] {"startValue"});

    assertEquals(new WorkflowExecution().setWorkflowId("workflowId"), execution);

    SignalWithStartWorkflowExecutionRequest request =
        requestFuture.getNow(null).getSignalWithStartRequest().getRequest();
    assertEquals(new WorkflowType().setName("type"), request.getWorkflowType());
    assertEquals("workflowId", request.getWorkflowId());
    assertEquals(1, request.getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, request.getTaskStartToCloseTimeoutSeconds());
    assertEquals("domain", request.getDomain());
    assertEquals("taskList", request.getTaskList().getName());
    assertEquals(
        "\"startValue\"",
        StandardCharsets.UTF_8.decode(ByteBuffer.wrap(request.getInput())).toString());
    assertEquals("signalName", request.getSignalName());
    assertEquals(
        "\"signalValue\"",
        StandardCharsets.UTF_8.decode(ByteBuffer.wrap(request.getSignalInput())).toString());
    assertNotNull(request.getRequestId());
  }

  @Test
  public void testEnqueueSignalWithStart_includesTracing() {
    CompletableFuture<WorkflowService.SignalWithStartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubSuccess(
            "WorkflowService::SignalWithStartWorkflowExecutionAsync",
            WorkflowService.SignalWithStartWorkflowExecutionAsync_args.class,
            new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                .setSuccess(new SignalWithStartWorkflowExecutionAsyncResponse()));

    WorkflowStub stub =
        client.newUntypedWorkflowStub(
            "type",
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());
    stub.enqueueSignalWithStart(
        "signalName", new Object[] {"signalValue"}, new Object[] {"startValue"});

    SignalWithStartWorkflowExecutionRequest request =
        requestFuture.getNow(null).getSignalWithStartRequest().getRequest();
    assertEquals(2, fakeService.getTracer().finishedSpans().size());
    MockSpan mockSpan =
        fakeService
            .getTracer()
            .finishedSpans()
            .stream()
            .filter(
                span ->
                    "cadence-SignalWithStartWorkflowExecutionAsync".equals(span.operationName()))
            .findFirst()
            .orElseThrow(
                () ->
                    new AssertionError(
                        "No span found for SignalWithStartWorkflowExecutionAsync:"
                            + fakeService.getTracer().finishedSpans()));
    assertEquals(
        mockSpan.context().toTraceId(),
        Charsets.UTF_8.decode(request.getHeader().getFields().get("traceid")).toString());
    assertEquals(
        mockSpan.context().toSpanId(),
        Charsets.UTF_8.decode(request.getHeader().getFields().get("spanid")).toString());
  }

  interface TestSignalWorkflow {
    @WorkflowMethod(
      taskList = "taskList",
      executionStartToCloseTimeoutSeconds = 1,
      taskStartToCloseTimeoutSeconds = 2
    )
    void test(String input);

    @SignalMethod
    void signal(String input);
  }

  @Test
  public void testEnqueueSignalWithStart_stronglyTyped() {
    CompletableFuture<WorkflowService.SignalWithStartWorkflowExecutionAsync_args> requestFuture =
        fakeService.stubSuccess(
            "WorkflowService::SignalWithStartWorkflowExecutionAsync",
            WorkflowService.SignalWithStartWorkflowExecutionAsync_args.class,
            new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                .setSuccess(new SignalWithStartWorkflowExecutionAsyncResponse()));

    TestSignalWorkflow stub =
        client.newWorkflowStub(
            TestSignalWorkflow.class,
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());

    BatchRequest batch = client.newSignalWithStartRequest();
    batch.add(stub::test, "startValue");
    batch.add(stub::signal, "signalValue");
    WorkflowExecution execution = client.enqueueSignalWithStart(batch);

    assertEquals(new WorkflowExecution().setWorkflowId("workflowId"), execution);

    SignalWithStartWorkflowExecutionRequest request =
        requestFuture.getNow(null).getSignalWithStartRequest().getRequest();
    assertEquals(new WorkflowType().setName("TestSignalWorkflow::test"), request.getWorkflowType());
    assertEquals("workflowId", request.getWorkflowId());
    assertEquals(1, request.getExecutionStartToCloseTimeoutSeconds());
    assertEquals(2, request.getTaskStartToCloseTimeoutSeconds());
    assertEquals("domain", request.getDomain());
    assertEquals("taskList", request.getTaskList().getName());
    assertEquals(
        "\"startValue\"",
        StandardCharsets.UTF_8.decode(ByteBuffer.wrap(request.getInput())).toString());
    assertEquals("TestSignalWorkflow::signal", request.getSignalName());
    assertEquals(
        "\"signalValue\"",
        StandardCharsets.UTF_8.decode(ByteBuffer.wrap(request.getSignalInput())).toString());
    assertNotNull(request.getRequestId());
  }

  @Test
  public void testEnqueueSignalWithStart_usesConsistentRequestId() {
    CompletableFuture<WorkflowService.SignalWithStartWorkflowExecutionAsync_args> firstAttempt =
        fakeService.stubError(
            "WorkflowService::SignalWithStartWorkflowExecutionAsync",
            WorkflowService.SignalWithStartWorkflowExecutionAsync_args.class,
            new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                .setServiceBusyError(new ServiceBusyError("try again later")));
    CompletableFuture<WorkflowService.SignalWithStartWorkflowExecutionAsync_args> secondAttempt =
        fakeService.stubSuccess(
            "WorkflowService::SignalWithStartWorkflowExecutionAsync",
            WorkflowService.SignalWithStartWorkflowExecutionAsync_args.class,
            new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                .setSuccess(new SignalWithStartWorkflowExecutionAsyncResponse()));

    TestSignalWorkflow stub =
        client.newWorkflowStub(
            TestSignalWorkflow.class,
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());

    BatchRequest batch = client.newSignalWithStartRequest();
    batch.add(stub::test, "startValue");
    batch.add(stub::signal, "signalValue");
    client.enqueueSignalWithStart(batch);

    assertTrue("first request was not made", firstAttempt.isDone());
    assertTrue("second request was not made", secondAttempt.isDone());
    String firstRequestId =
        firstAttempt.getNow(null).getSignalWithStartRequest().getRequest().getRequestId();
    String secondRequestId =
        secondAttempt.getNow(null).getSignalWithStartRequest().getRequest().getRequestId();
    assertNotNull("first request must have a request id", firstRequestId);
    assertEquals(firstRequestId, secondRequestId);
  }

  @Test
  public void testEnqueueStart_usesConsistentRequestId() {
    CompletableFuture<WorkflowService.StartWorkflowExecutionAsync_args> firstAttempt =
        fakeService.stubError(
            "WorkflowService::StartWorkflowExecutionAsync",
            WorkflowService.StartWorkflowExecutionAsync_args.class,
            new WorkflowService.StartWorkflowExecutionAsync_result()
                .setServiceBusyError(new ServiceBusyError("try again later")));
    CompletableFuture<WorkflowService.StartWorkflowExecutionAsync_args> secondAttempt =
        fakeService.stubSuccess(
            "WorkflowService::StartWorkflowExecutionAsync",
            WorkflowService.StartWorkflowExecutionAsync_args.class,
            new WorkflowService.StartWorkflowExecutionAsync_result()
                .setSuccess(new StartWorkflowExecutionAsyncResponse()));

    TestWorkflow stub =
        client.newWorkflowStub(
            TestWorkflow.class,
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());

    WorkflowClient.enqueueStart(stub::test, "input");

    assertTrue("first request was not made", firstAttempt.isDone());
    assertTrue("second request was not made", secondAttempt.isDone());
    String firstRequestId = firstAttempt.getNow(null).getStartRequest().getRequest().getRequestId();
    String secondRequestId =
        secondAttempt.getNow(null).getStartRequest().getRequest().getRequestId();
    assertNotNull("first request must have a request id", firstRequestId);
    assertEquals(firstRequestId, secondRequestId);
  }

  @Test
  public void testStartWorkflow_usesConsistentRequestId() {
    CompletableFuture<WorkflowService.StartWorkflowExecution_args> firstAttempt =
        fakeService.stubError(
            "WorkflowService::StartWorkflowExecution",
            WorkflowService.StartWorkflowExecution_args.class,
            new WorkflowService.StartWorkflowExecution_result()
                .setServiceBusyError(new ServiceBusyError("try again later")));
    CompletableFuture<WorkflowService.StartWorkflowExecution_args> secondAttempt =
        fakeService.stubSuccess(
            "WorkflowService::StartWorkflowExecution",
            WorkflowService.StartWorkflowExecution_args.class,
            new WorkflowService.StartWorkflowExecution_result()
                .setSuccess(new StartWorkflowExecutionResponse().setRunId("foo")));

    TestWorkflow stub =
        client.newWorkflowStub(
            TestWorkflow.class,
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());

    WorkflowClient.start(stub::test, "input");

    assertTrue("first request was not made", firstAttempt.isDone());
    assertTrue("second request was not made", secondAttempt.isDone());
    String firstRequestId = firstAttempt.getNow(null).getStartRequest().requestId;
    String secondRequestId = secondAttempt.getNow(null).getStartRequest().requestId;
    assertNotNull("first request must have a request id", firstRequestId);
    assertEquals(firstRequestId, secondRequestId);
  }

  @Test
  public void testSignalWithStartWorkflow_usesConsistentRequestId() {
    CompletableFuture<WorkflowService.SignalWithStartWorkflowExecution_args> firstAttempt =
        fakeService.stubError(
            "WorkflowService::SignalWithStartWorkflowExecution",
            WorkflowService.SignalWithStartWorkflowExecution_args.class,
            new WorkflowService.SignalWithStartWorkflowExecution_result()
                .setServiceBusyError(new ServiceBusyError("try again later")));
    CompletableFuture<WorkflowService.SignalWithStartWorkflowExecution_args> secondAttempt =
        fakeService.stubSuccess(
            "WorkflowService::SignalWithStartWorkflowExecution",
            WorkflowService.SignalWithStartWorkflowExecution_args.class,
            new WorkflowService.SignalWithStartWorkflowExecution_result()
                .setSuccess(new StartWorkflowExecutionResponse().setRunId("foo")));

    TestSignalWorkflow stub =
        client.newWorkflowStub(
            TestSignalWorkflow.class,
            new WorkflowOptions.Builder()
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(1))
                .setTaskStartToCloseTimeout(Duration.ofSeconds(2))
                .setWorkflowId("workflowId")
                .setTaskList("taskList")
                .build());

    BatchRequest batch = client.newSignalWithStartRequest();
    batch.add(stub::test, "startValue");
    batch.add(stub::signal, "signalValue");
    client.signalWithStart(batch);

    assertTrue("first request was not made", firstAttempt.isDone());
    assertTrue("second request was not made", secondAttempt.isDone());
    String firstRequestId = firstAttempt.getNow(null).getSignalWithStartRequest().requestId;
    String secondRequestId = secondAttempt.getNow(null).getSignalWithStartRequest().requestId;
    assertNotNull("first request must have a request id", firstRequestId);
    assertEquals(firstRequestId, secondRequestId);
  }

  @Test
  public void testSignalWorkflow_usesConsistentRequestId() {
    CompletableFuture<WorkflowService.SignalWorkflowExecution_args> firstAttempt =
        fakeService.stubError(
            "WorkflowService::SignalWorkflowExecution",
            WorkflowService.SignalWorkflowExecution_args.class,
            new WorkflowService.SignalWorkflowExecution_result()
                .setServiceBusyError(new ServiceBusyError("try again later")));
    CompletableFuture<WorkflowService.SignalWorkflowExecution_args> secondAttempt =
        fakeService.stubSuccess(
            "WorkflowService::SignalWorkflowExecution",
            WorkflowService.SignalWorkflowExecution_args.class,
            new WorkflowService.SignalWorkflowExecution_result());

    TestSignalWorkflow stub = client.newWorkflowStub(TestSignalWorkflow.class, "workflowId");

    stub.signal("signalValue");

    assertTrue("first request was not made", firstAttempt.isDone());
    assertTrue("second request was not made", secondAttempt.isDone());
    String firstRequestId = firstAttempt.getNow(null).getSignalRequest().getRequestId();
    String secondRequestId = secondAttempt.getNow(null).getSignalRequest().getRequestId();
    assertNotNull("first request must have a request id", firstRequestId);
    assertEquals(firstRequestId, secondRequestId);
  }

  @Test
  public void testCancel_usesConsistentRequestId() {
    CompletableFuture<WorkflowService.RequestCancelWorkflowExecution_args> firstAttempt =
        fakeService.stubError(
            "WorkflowService::RequestCancelWorkflowExecution",
            WorkflowService.RequestCancelWorkflowExecution_args.class,
            new WorkflowService.RequestCancelWorkflowExecution_result()
                .setServiceBusyError(new ServiceBusyError("try again later")));
    CompletableFuture<WorkflowService.RequestCancelWorkflowExecution_args> secondAttempt =
        fakeService.stubSuccess(
            "WorkflowService::RequestCancelWorkflowExecution",
            WorkflowService.RequestCancelWorkflowExecution_args.class,
            new WorkflowService.RequestCancelWorkflowExecution_result());

    WorkflowStub stub =
        client.newUntypedWorkflowStub("workflowId", Optional.empty(), Optional.empty());
    stub.cancel();

    assertTrue("first request was not made", firstAttempt.isDone());
    assertTrue("second request was not made", secondAttempt.isDone());
    String firstRequestId = firstAttempt.getNow(null).getCancelRequest().getRequestId();
    String secondRequestId = secondAttempt.getNow(null).getCancelRequest().getRequestId();
    assertNotNull("first request must have a request id", firstRequestId);
    assertEquals(firstRequestId, secondRequestId);
  }
}
