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

package com.uber.cadence.internal.compatibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.uber.cadence.AccessDeniedError;
import com.uber.cadence.RefreshWorkflowTasksRequest;
import com.uber.cadence.SignalWithStartWorkflowExecutionAsyncRequest;
import com.uber.cadence.SignalWithStartWorkflowExecutionAsyncResponse;
import com.uber.cadence.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionAsyncRequest;
import com.uber.cadence.StartWorkflowExecutionAsyncResponse;
import com.uber.cadence.StartWorkflowExecutionRequest;
import com.uber.cadence.StartWorkflowExecutionResponse;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.api.v1.DomainAPIGrpc;
import com.uber.cadence.api.v1.Header;
import com.uber.cadence.api.v1.HealthResponse;
import com.uber.cadence.api.v1.MetaAPIGrpc;
import com.uber.cadence.api.v1.VisibilityAPIGrpc;
import com.uber.cadence.api.v1.WorkerAPIGrpc;
import com.uber.cadence.api.v1.WorkflowAPIGrpc;
import com.uber.cadence.internal.compatibility.proto.RequestMapper;
import com.uber.cadence.internal.compatibility.proto.serviceclient.IGrpcServiceStubs;
import com.uber.cadence.internal.compatibility.thrift.ResponseMapper;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.io.Charsets;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class Thrift2ProtoAdapterTest {
  private static final Metadata.Key<String> AUTHORIZATION_HEADER_KEY =
      Metadata.Key.of("cadence-authorization", Metadata.ASCII_STRING_MARSHALLER);
  private static final StatusRuntimeException GRPC_ACCESS_DENIED =
      new StatusRuntimeException(Status.PERMISSION_DENIED);

  @Rule public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
  private final MockTracer tracer = new MockTracer();
  private final FakeGrpcServer stubs = new FakeGrpcServer();
  private IWorkflowService client;
  private IWorkflowService tracingClient;

  @Before
  public void setup() {
    grpcCleanup.register(
        stubs.createServer(
            DomainAPIGrpc.getServiceDescriptor(),
            VisibilityAPIGrpc.getServiceDescriptor(),
            WorkflowAPIGrpc.getServiceDescriptor(),
            WorkerAPIGrpc.getServiceDescriptor(),
            MetaAPIGrpc.getServiceDescriptor()));
    ManagedChannel clientChannel = grpcCleanup.register(stubs.createClient());
    Logger logger =
        (Logger)
            LoggerFactory.getLogger(
                "com.uber.cadence.internal.compatibility.proto.serviceclient.GrpcServiceStubs");
    logger.setLevel(Level.TRACE);
    client =
        new Thrift2ProtoAdapter(
            IGrpcServiceStubs.newInstance(
                ClientOptions.newBuilder()
                    .setAuthorizationProvider("foo"::getBytes)
                    .setGRPCChannel(clientChannel)
                    .build()));
    tracingClient =
        new Thrift2ProtoAdapter(
            IGrpcServiceStubs.newInstance(
                ClientOptions.newBuilder()
                    .setAuthorizationProvider("foo"::getBytes)
                    .setTracer(tracer)
                    .setGRPCChannel(clientChannel)
                    .build()));
  }

  @Test
  public void testStartWorkflowExecution() throws Exception {
    CompletableFuture<com.uber.cadence.api.v1.StartWorkflowExecutionRequest> protoRequest =
        stub(
            WorkflowAPIGrpc.getStartWorkflowExecutionMethod(),
            ProtoObjects.START_WORKFLOW_EXECUTION_RESPONSE);
    StartWorkflowExecutionRequest request = ThriftObjects.START_WORKFLOW_EXECUTION.deepCopy();
    // Test that a request ID will be set.
    request.unsetRequestId();

    StartWorkflowExecutionResponse response = client.StartWorkflowExecution(request);

    assertEquals(
        ResponseMapper.startWorkflowExecutionResponse(
            ProtoObjects.START_WORKFLOW_EXECUTION_RESPONSE),
        response);

    assertNotNull(request.getRequestId());
    assertEquals(RequestMapper.startWorkflowExecutionRequest(request), protoRequest.join());
  }

  @Test
  public void testStartWorkflowExecution_tracing() throws Exception {
    CompletableFuture<com.uber.cadence.api.v1.StartWorkflowExecutionRequest> protoRequest =
        stub(
            WorkflowAPIGrpc.getStartWorkflowExecutionMethod(),
            ProtoObjects.START_WORKFLOW_EXECUTION_RESPONSE);
    StartWorkflowExecutionRequest request = ThriftObjects.START_WORKFLOW_EXECUTION.deepCopy();

    tracingClient.StartWorkflowExecution(request);

    assertTracingHeaders(protoRequest.join().getHeader());
  }

  @Test
  public void testStartWorkflowExecution_error() {
    stubWithAccessDenied(WorkflowAPIGrpc.getStartWorkflowExecutionMethod());

    assertThrows(
        AccessDeniedError.class,
        () -> client.StartWorkflowExecution(ThriftObjects.START_WORKFLOW_EXECUTION));
  }

  @Test
  public void testStartWorkflowExecutionAsync() throws Exception {
    CompletableFuture<com.uber.cadence.api.v1.StartWorkflowExecutionAsyncRequest> protoRequest =
        stub(
            WorkflowAPIGrpc.getStartWorkflowExecutionAsyncMethod(),
            ProtoObjects.START_WORKFLOW_EXECUTION_ASYNC_RESPONSE);
    StartWorkflowExecutionAsyncRequest request =
        ThriftObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST.deepCopy();
    // Test that a request ID will be set.
    request.getRequest().unsetRequestId();

    StartWorkflowExecutionAsyncResponse response = client.StartWorkflowExecutionAsync(request);

    assertEquals(
        ResponseMapper.startWorkflowExecutionAsyncResponse(
            ProtoObjects.START_WORKFLOW_EXECUTION_ASYNC_RESPONSE),
        response);

    assertNotNull(request.getRequest().getRequestId());
    assertEquals(RequestMapper.startWorkflowExecutionAsyncRequest(request), protoRequest.join());
  }

  @Test
  public void testStartWorkflowExecutionAsync_tracing() throws Exception {
    CompletableFuture<com.uber.cadence.api.v1.StartWorkflowExecutionAsyncRequest> protoRequest =
        stub(
            WorkflowAPIGrpc.getStartWorkflowExecutionAsyncMethod(),
            ProtoObjects.START_WORKFLOW_EXECUTION_ASYNC_RESPONSE);
    StartWorkflowExecutionAsyncRequest request =
        ThriftObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST.deepCopy();

    tracingClient.StartWorkflowExecutionAsync(request);

    assertTracingHeaders(protoRequest.join().getRequest().getHeader());
  }

  @Test
  public void testStartWorkflowExecutionAsync_error() {
    stubWithAccessDenied(WorkflowAPIGrpc.getStartWorkflowExecutionAsyncMethod());

    assertThrows(
        AccessDeniedError.class,
        () ->
            client.StartWorkflowExecutionAsync(
                ThriftObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST));
  }

  @Test
  public void testSignalWithStartWorkflowExecution() throws Exception {
    CompletableFuture<com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionRequest>
        protoRequest =
            stub(
                WorkflowAPIGrpc.getSignalWithStartWorkflowExecutionMethod(),
                ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_RESPONSE);
    SignalWithStartWorkflowExecutionRequest request =
        ThriftObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION.deepCopy();
    // Test that a request ID will be set.
    request.unsetRequestId();

    StartWorkflowExecutionResponse response = client.SignalWithStartWorkflowExecution(request);

    assertEquals(
        ResponseMapper.signalWithStartWorkflowExecutionResponse(
            ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_RESPONSE),
        response);

    assertNotNull(request.getRequestId());
    assertEquals(
        RequestMapper.signalWithStartWorkflowExecutionRequest(request), protoRequest.join());
  }

  @Test
  public void testSignalWithStartWorkflowExecution_tracing() throws Exception {
    CompletableFuture<com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionRequest>
        protoRequest =
            stub(
                WorkflowAPIGrpc.getSignalWithStartWorkflowExecutionMethod(),
                ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_RESPONSE);
    SignalWithStartWorkflowExecutionRequest request =
        ThriftObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION.deepCopy();

    tracingClient.SignalWithStartWorkflowExecution(request);

    assertTracingHeaders(protoRequest.join().getStartRequest().getHeader());
  }

  @Test
  public void testSignalWithStartWorkflowExecution_error() {
    stubWithAccessDenied(WorkflowAPIGrpc.getSignalWithStartWorkflowExecutionMethod());

    assertThrows(
        AccessDeniedError.class,
        () ->
            client.SignalWithStartWorkflowExecution(
                ThriftObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION));
  }

  @Test
  public void testSignalWithStartWorkflowAsyncExecution() throws Exception {
    CompletableFuture<com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionAsyncRequest>
        protoRequest =
            stub(
                WorkflowAPIGrpc.getSignalWithStartWorkflowExecutionAsyncMethod(),
                ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_RESPONSE);
    SignalWithStartWorkflowExecutionAsyncRequest request =
        ThriftObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_REQUEST.deepCopy();
    // Test that a request ID will be set.
    request.getRequest().unsetRequestId();

    SignalWithStartWorkflowExecutionAsyncResponse response =
        client.SignalWithStartWorkflowExecutionAsync(request);

    assertEquals(
        ResponseMapper.signalWithStartWorkflowExecutionAsyncResponse(
            ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_RESPONSE),
        response);

    assertNotNull(request.getRequest().getRequestId());
    assertEquals(
        RequestMapper.signalWithStartWorkflowExecutionAsyncRequest(request), protoRequest.join());
  }

  @Test
  public void testSignalWithStartWorkflowAsyncExecution_tracing() throws Exception {
    CompletableFuture<com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionAsyncRequest>
        protoRequest =
            stub(
                WorkflowAPIGrpc.getSignalWithStartWorkflowExecutionAsyncMethod(),
                ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_RESPONSE);
    SignalWithStartWorkflowExecutionAsyncRequest request =
        ThriftObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_REQUEST.deepCopy();

    tracingClient.SignalWithStartWorkflowExecutionAsync(request);

    assertTracingHeaders(protoRequest.join().getRequest().getStartRequest().getHeader());
  }

  @Test
  public void testSignalWithStartWorkflowAsyncExecution_error() {
    stubWithAccessDenied(WorkflowAPIGrpc.getSignalWithStartWorkflowExecutionAsyncMethod());

    assertThrows(
        AccessDeniedError.class,
        () ->
            client.SignalWithStartWorkflowExecutionAsync(
                ThriftObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_REQUEST));
  }

  @Test
  public void testCountWorkflowExecutions() throws Exception {
    testHelper(
        VisibilityAPIGrpc.getCountWorkflowExecutionsMethod(),
        ProtoObjects.COUNT_WORKFLOW_EXECUTIONS_REQUEST,
        ProtoObjects.COUNT_WORKFLOW_EXECUTIONS_RESPONSE,
        IWorkflowService::CountWorkflowExecutions,
        ThriftObjects.COUNT_WORKFLOW_EXECUTIONS_REQUEST,
        ThriftObjects.COUNT_WORKFLOW_EXECUTIONS_RESPONSE);
  }

  @Test
  public void testListWorkflowExecutions() throws Exception {
    testHelper(
        VisibilityAPIGrpc.getListWorkflowExecutionsMethod(),
        ProtoObjects.LIST_WORKFLOW_EXECUTIONS_REQUEST,
        ProtoObjects.LIST_WORKFLOW_EXECUTIONS_RESPONSE,
        IWorkflowService::ListWorkflowExecutions,
        ThriftObjects.LIST_WORKFLOW_EXECUTIONS_REQUEST,
        ThriftObjects.LIST_WORKFLOW_EXECUTIONS_RESPONSE);
  }

  @Test
  public void testListOpenWorkflowExecutions() throws Exception {
    testHelper(
        VisibilityAPIGrpc.getListOpenWorkflowExecutionsMethod(),
        ProtoObjects.LIST_OPEN_WORKFLOW_EXECUTIONS_REQUEST,
        ProtoObjects.LIST_OPEN_WORKFLOW_EXECUTIONS_RESPONSE,
        IWorkflowService::ListOpenWorkflowExecutions,
        ThriftObjects.LIST_OPEN_WORKFLOW_EXECUTIONS_REQUEST,
        ThriftObjects.LIST_OPEN_WORKFLOW_EXECUTIONS_RESPONSE);
  }

  @Test
  public void testListClosedWorkflowExecutions() throws Exception {
    testHelper(
        VisibilityAPIGrpc.getListClosedWorkflowExecutionsMethod(),
        ProtoObjects.LIST_CLOSED_WORKFLOW_EXECUTIONS_REQUEST,
        ProtoObjects.LIST_CLOSED_WORKFLOW_EXECUTIONS_RESPONSE,
        IWorkflowService::ListClosedWorkflowExecutions,
        ThriftObjects.LIST_CLOSED_WORKFLOW_EXECUTIONS_REQUEST,
        ThriftObjects.LIST_CLOSED_WORKFLOW_EXECUTIONS_RESPONSE);
  }

  @Test
  public void testListArchivedWorkflowExecutions() throws Exception {
    testHelper(
        VisibilityAPIGrpc.getListArchivedWorkflowExecutionsMethod(),
        ProtoObjects.LIST_ARCHIVED_WORKFLOW_EXECUTIONS_REQUEST,
        ProtoObjects.LIST_ARCHIVED_WORKFLOW_EXECUTIONS_RESPONSE,
        IWorkflowService::ListArchivedWorkflowExecutions,
        ThriftObjects.LIST_ARCHIVED_WORKFLOW_EXECUTIONS_REQUEST,
        ThriftObjects.LIST_ARCHIVED_WORKFLOW_EXECUTIONS_RESPONSE);
  }

  @Test
  public void testScanWorkflowExecutions() throws Exception {
    testHelper(
        VisibilityAPIGrpc.getScanWorkflowExecutionsMethod(),
        ProtoObjects.SCAN_WORKFLOW_EXECUTIONS_REQUEST,
        ProtoObjects.SCAN_WORKFLOW_EXECUTIONS_RESPONSE,
        IWorkflowService::ScanWorkflowExecutions,
        ThriftObjects.LIST_WORKFLOW_EXECUTIONS_REQUEST,
        ThriftObjects.LIST_WORKFLOW_EXECUTIONS_RESPONSE);
  }

  @Test
  public void testGetSearchAttributes() throws Exception {
    testHelper(
        VisibilityAPIGrpc.getGetSearchAttributesMethod(),
        ProtoObjects.GET_SEARCH_ATTRIBUTES_REQUEST,
        ProtoObjects.GET_SEARCH_ATTRIBUTES_RESPONSE,
        WorkflowService.Iface::GetSearchAttributes,
        ThriftObjects.GET_SEARCH_ATTRIBUTES_RESPONSE);
  }

  @Test
  public void testRegisterDomain() throws Exception {
    testHelper(
        DomainAPIGrpc.getRegisterDomainMethod(),
        ProtoObjects.REGISTER_DOMAIN_REQUEST,
        ProtoObjects.REGISTER_DOMAIN_RESPONSE,
        IWorkflowService::RegisterDomain,
        ThriftObjects.REGISTER_DOMAIN_REQUEST);
  }

  @Test
  public void testDescribeDomain() throws Exception {
    testHelper(
        DomainAPIGrpc.getDescribeDomainMethod(),
        ProtoObjects.DESCRIBE_DOMAIN_BY_ID_REQUEST,
        ProtoObjects.DESCRIBE_DOMAIN_RESPONSE,
        IWorkflowService::DescribeDomain,
        ThriftObjects.DESCRIBE_DOMAIN_BY_ID_REQUEST,
        ThriftObjects.DESCRIBE_DOMAIN_RESPONSE);
  }

  @Test
  public void testListDomains() throws Exception {
    testHelper(
        DomainAPIGrpc.getListDomainsMethod(),
        ProtoObjects.LIST_DOMAINS_REQUEST,
        ProtoObjects.LIST_DOMAINS_RESPONSE,
        IWorkflowService::ListDomains,
        ThriftObjects.LIST_DOMAINS_REQUEST,
        ThriftObjects.LIST_DOMAINS_RESPONSE);
  }

  @Test
  public void testUpdateDomain() throws Exception {
    testHelper(
        DomainAPIGrpc.getUpdateDomainMethod(),
        ProtoObjects.UPDATE_DOMAIN_REQUEST,
        ProtoObjects.UPDATE_DOMAIN_RESPONSE,
        IWorkflowService::UpdateDomain,
        ThriftObjects.UPDATE_DOMAIN_REQUEST,
        ThriftObjects.UPDATE_DOMAIN_RESPONSE);
  }

  @Test
  public void testDeprecateDomain() throws Exception {
    testHelper(
        DomainAPIGrpc.getDeprecateDomainMethod(),
        ProtoObjects.DEPRECATE_DOMAIN_REQUEST,
        ProtoObjects.DEPRECATE_DOMAIN_RESPONSE,
        IWorkflowService::DeprecateDomain,
        ThriftObjects.DEPRECATE_DOMAIN_REQUEST);
  }

  @Test
  public void testSignalWorkflowExecution() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getSignalWorkflowExecutionMethod(),
        ProtoObjects.SIGNAL_WORKFLOW_EXECUTION_REQUEST,
        ProtoObjects.SIGNAL_WORKFLOW_EXECUTION_RESPONSE,
        IWorkflowService::SignalWorkflowExecution,
        ThriftObjects.SIGNAL_WORKFLOW_EXECUTION_REQUEST);
  }

  @Test
  public void testResetWorkflowExecution() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getResetWorkflowExecutionMethod(),
        ProtoObjects.RESET_WORKFLOW_EXECUTION_REQUEST,
        ProtoObjects.RESET_WORKFLOW_EXECUTION_RESPONSE,
        IWorkflowService::ResetWorkflowExecution,
        ThriftObjects.RESET_WORKFLOW_EXECUTION_REQUEST,
        ThriftObjects.RESET_WORKFLOW_EXECUTION_RESPONSE);
  }

  @Test
  public void testRequestCancelWorkflowExecution() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getRequestCancelWorkflowExecutionMethod(),
        ProtoObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST,
        ProtoObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_RESPONSE,
        IWorkflowService::RequestCancelWorkflowExecution,
        ThriftObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST);
  }

  @Test
  public void testTerminateWorkflowExecution() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getTerminateWorkflowExecutionMethod(),
        ProtoObjects.TERMINATE_WORKFLOW_EXECUTION_REQUEST,
        ProtoObjects.TERMINATE_WORKFLOW_EXECUTION_RESPONSE,
        IWorkflowService::TerminateWorkflowExecution,
        ThriftObjects.TERMINATE_WORKFLOW_EXECUTION_REQUEST);
  }

  @Test
  public void testDescribeWorkflowExecution() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getDescribeWorkflowExecutionMethod(),
        ProtoObjects.DESCRIBE_WORKFLOW_EXECUTION_REQUEST,
        ProtoObjects.DESCRIBE_WORKFLOW_EXECUTION_RESPONSE,
        IWorkflowService::DescribeWorkflowExecution,
        ThriftObjects.DESCRIBE_WORKFLOW_EXECUTION_REQUEST,
        ThriftObjects.DESCRIBE_WORKFLOW_EXECUTION_RESPONSE);
  }

  @Test
  public void testQueryWorkflow() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getQueryWorkflowMethod(),
        ProtoObjects.QUERY_WORKFLOW_REQUEST,
        ProtoObjects.QUERY_WORKFLOW_RESPONSE,
        IWorkflowService::QueryWorkflow,
        ThriftObjects.QUERY_WORKFLOW_REQUEST,
        ThriftObjects.QUERY_WORKFLOW_RESPONSE);
  }

  @Test
  public void testDescribeTaskList() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getDescribeTaskListMethod(),
        ProtoObjects.DESCRIBE_TASK_LIST_REQUEST,
        ProtoObjects.DESCRIBE_TASK_LIST_RESPONSE,
        IWorkflowService::DescribeTaskList,
        ThriftObjects.DESCRIBE_TASK_LIST_REQUEST,
        ThriftObjects.DESCRIBE_TASK_LIST_RESPONSE);
  }

  @Test
  public void testListTaskListPartitions() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getListTaskListPartitionsMethod(),
        ProtoObjects.LIST_TASK_LIST_PARTITIONS_REQUEST,
        ProtoObjects.LIST_TASK_LIST_PARTITIONS_RESPONSE,
        IWorkflowService::ListTaskListPartitions,
        ThriftObjects.LIST_TASK_LIST_PARTITIONS_REQUEST,
        ThriftObjects.LIST_TASK_LIST_PARTITIONS_RESPONSE);
  }

  @Test
  public void testGetClusterInfo() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getGetClusterInfoMethod(),
        ProtoObjects.GET_CLUSTER_INFO_REQUEST,
        ProtoObjects.GET_CLUSTER_INFO_RESPONSE,
        WorkflowService.Iface::GetClusterInfo,
        ThriftObjects.CLUSTER_INFO);
  }

  @Test
  public void testGetWorkflowExecutionHistory() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getGetWorkflowExecutionHistoryMethod(),
        ProtoObjects.GET_WORKFLOW_EXECUTION_HISTORY_REQUEST,
        ProtoObjects.GET_WORKFLOW_EXECUTION_HISTORY_RESPONSE,
        IWorkflowService::GetWorkflowExecutionHistory,
        ThriftObjects.GET_WORKFLOW_EXECUTION_HISTORY_REQUEST,
        ThriftObjects.GET_WORKFLOW_EXECUTION_HISTORY_RESPONSE);
  }

  @Test
  public void testRefreshWorkflowTasks() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getRefreshWorkflowTasksMethod(),
        ProtoObjects.REFRESH_WORKFLOW_TASKS_REQUEST,
        ProtoObjects.REFRESH_WORKFLOW_TASKS_RESPONSE,
        IWorkflowService::RefreshWorkflowTasks,
        new RefreshWorkflowTasksRequest());
  }

  @Test
  public void testPollForDecisionTask() throws Exception {
    testHelper(
        WorkerAPIGrpc.getPollForDecisionTaskMethod(),
        ProtoObjects.POLL_FOR_DECISION_TASK_REQUEST,
        ProtoObjects.POLL_FOR_DECISION_TASK_RESPONSE,
        IWorkflowService::PollForDecisionTask,
        ThriftObjects.POLL_FOR_DECISION_TASK_REQUEST,
        ThriftObjects.POLL_FOR_DECISION_TASK_RESPONSE);
  }

  @Test
  public void testRespondDecisionTaskCompleted() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRespondDecisionTaskCompletedMethod(),
        ProtoObjects.RESPOND_DECISION_TASK_COMPLETED_REQUEST,
        ProtoObjects.RESPOND_DECISION_TASK_COMPLETED_RESPONSE,
        IWorkflowService::RespondDecisionTaskCompleted,
        ThriftObjects.RESPOND_DECISION_TASK_COMPLETED_REQUEST,
        ThriftObjects.RESPOND_DECISION_TASK_COMPLETED_RESPONSE);
  }

  @Test
  public void testRespondDecisionTaskFailed() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRespondDecisionTaskFailedMethod(),
        ProtoObjects.RESPOND_DECISION_TASK_FAILED_REQUEST,
        ProtoObjects.RESPOND_DECISION_TASK_FAILED_RESPONSE,
        IWorkflowService::RespondDecisionTaskFailed,
        ThriftObjects.RESPOND_DECISION_TASK_FAILED_REQUEST);
  }

  @Test
  public void testPollForActivityTask() throws Exception {
    testHelper(
        WorkerAPIGrpc.getPollForActivityTaskMethod(),
        ProtoObjects.POLL_FOR_ACTIVITY_TASK_REQUEST,
        ProtoObjects.POLL_FOR_ACTIVITY_TASK_RESPONSE,
        IWorkflowService::PollForActivityTask,
        ThriftObjects.POLL_FOR_ACTIVITY_TASK_REQUEST,
        ThriftObjects.POLL_FOR_ACTIVITY_TASK_RESPONSE);
  }

  @Test
  public void testRespondActivityTaskCompleted() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRespondActivityTaskCompletedMethod(),
        ProtoObjects.RESPOND_ACTIVITY_TASK_COMPLETED_REQUEST,
        ProtoObjects.RESPOND_ACTIVITY_TASK_COMPLETED_RESPONSE,
        IWorkflowService::RespondActivityTaskCompleted,
        ThriftObjects.RESPOND_ACTIVITY_TASK_COMPLETED_REQUEST);
  }

  @Test
  public void testRespondActivityTaskCompletedById() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRespondActivityTaskCompletedByIDMethod(),
        ProtoObjects.RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_REQUEST,
        ProtoObjects.RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_RESPONSE,
        IWorkflowService::RespondActivityTaskCompletedByID,
        ThriftObjects.RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_REQUEST);
  }

  @Test
  public void testRespondActivityTaskFailed() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRespondActivityTaskFailedMethod(),
        ProtoObjects.RESPOND_ACTIVITY_TASK_FAILED_REQUEST,
        ProtoObjects.RESPOND_ACTIVITY_TASK_FAILED_RESPONSE,
        IWorkflowService::RespondActivityTaskFailed,
        ThriftObjects.RESPOND_ACTIVITY_TASK_FAILED_REQUEST);
  }

  @Test
  public void testRespondActivityTaskFailedById() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRespondActivityTaskFailedByIDMethod(),
        ProtoObjects.RESPOND_ACTIVITY_TASK_FAILED_BY_ID_REQUEST,
        ProtoObjects.RESPOND_ACTIVITY_TASK_FAILED_BY_ID_RESPONSE,
        IWorkflowService::RespondActivityTaskFailedByID,
        ThriftObjects.RESPOND_ACTIVITY_TASK_FAILED_BY_ID_REQUEST);
  }

  @Test
  public void testRespondActivityTaskCanceled() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRespondActivityTaskCanceledMethod(),
        ProtoObjects.RESPOND_ACTIVITY_TASK_CANCELED_REQUEST,
        ProtoObjects.RESPOND_ACTIVITY_TASK_CANCELED_RESPONSE,
        IWorkflowService::RespondActivityTaskCanceled,
        ThriftObjects.RESPOND_ACTIVITY_TASK_CANCELED_REQUEST);
  }

  @Test
  public void testRespondActivityTaskCanceledById() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRespondActivityTaskCanceledByIDMethod(),
        ProtoObjects.RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_REQUEST,
        ProtoObjects.RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_RESPONSE,
        IWorkflowService::RespondActivityTaskCanceledByID,
        ThriftObjects.RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_REQUEST);
  }

  @Test
  public void testRecordActivityTaskHeartbeat() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRecordActivityTaskHeartbeatMethod(),
        ProtoObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_REQUEST,
        ProtoObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_RESPONSE,
        IWorkflowService::RecordActivityTaskHeartbeat,
        ThriftObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_REQUEST,
        ThriftObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_RESPONSE);
  }

  @Test
  public void testRecordActivityTaskHeartbeatById() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRecordActivityTaskHeartbeatByIDMethod(),
        ProtoObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_REQUEST,
        ProtoObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_RESPONSE,
        IWorkflowService::RecordActivityTaskHeartbeatByID,
        ThriftObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_REQUEST,
        ThriftObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_RESPONSE);
  }

  @Test
  public void testRespondQueryTaskCompleted() throws Exception {
    testHelper(
        WorkerAPIGrpc.getRespondQueryTaskCompletedMethod(),
        ProtoObjects.RESPOND_QUERY_TASK_COMPLETED_REQUEST,
        ProtoObjects.RESPOND_QUERY_TASK_COMPLETED_RESPONSE,
        IWorkflowService::RespondQueryTaskCompleted,
        ThriftObjects.RESPOND_QUERY_TASK_COMPLETED_REQUEST);
  }

  @Test
  public void testResetStickyTaskList() throws Exception {
    testHelper(
        WorkerAPIGrpc.getResetStickyTaskListMethod(),
        ProtoObjects.RESET_STICKY_TASK_LIST_REQUEST,
        ProtoObjects.RESET_STICKY_TASK_LIST_RESPONSE,
        IWorkflowService::ResetStickyTaskList,
        ThriftObjects.RESET_STICKY_TASK_LIST_REQUEST);
  }

  @Test
  public void testAsyncSignalWorkflowExecution() throws Exception {
    testHelperAsync(
        WorkflowAPIGrpc.getSignalWorkflowExecutionMethod(),
        ProtoObjects.SIGNAL_WORKFLOW_EXECUTION_REQUEST,
        ProtoObjects.SIGNAL_WORKFLOW_EXECUTION_RESPONSE,
        IWorkflowService::SignalWorkflowExecution,
        ThriftObjects.SIGNAL_WORKFLOW_EXECUTION_REQUEST,
        null);
  }

  @Test
  public void testAsyncStartWorkflowExecutionAsync() throws Exception {
    testHelperAsync(
        WorkflowAPIGrpc.getStartWorkflowExecutionAsyncMethod(),
        ProtoObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
        ProtoObjects.START_WORKFLOW_EXECUTION_ASYNC_RESPONSE,
        IWorkflowService::StartWorkflowExecutionAsync,
        ThriftObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
        ThriftObjects.START_WORKFLOW_EXECUTION_ASYNC_RESPONSE);
  }

  @Test
  public void testStartWorkflowExecutionWithTimeout() throws Exception {
    testHelperAsync(
        WorkflowAPIGrpc.getStartWorkflowExecutionMethod(),
        ProtoObjects.START_WORKFLOW_EXECUTION,
        ProtoObjects.START_WORKFLOW_EXECUTION_RESPONSE,
        (service, request, handler) ->
            service.StartWorkflowExecutionWithTimeout(request, handler, 1000L),
        ThriftObjects.START_WORKFLOW_EXECUTION,
        ThriftObjects.START_WORKFLOW_EXECUTION_RESPONSE);
  }

  @Test
  public void testStartWorkflowExecutionAsyncWithTimeout() throws Exception {
    testHelperAsync(
        WorkflowAPIGrpc.getStartWorkflowExecutionAsyncMethod(),
        ProtoObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
        ProtoObjects.START_WORKFLOW_EXECUTION_ASYNC_RESPONSE,
        (service, request, handler) ->
            service.StartWorkflowExecutionAsyncWithTimeout(request, handler, 1000L),
        ThriftObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
        ThriftObjects.START_WORKFLOW_EXECUTION_ASYNC_RESPONSE);
  }

  @Test
  public void testGetWorkflowExecutionHistoryWithTimeout() throws Exception {
    testHelper(
        WorkflowAPIGrpc.getGetWorkflowExecutionHistoryMethod(),
        ProtoObjects.GET_WORKFLOW_EXECUTION_HISTORY_REQUEST,
        ProtoObjects.GET_WORKFLOW_EXECUTION_HISTORY_RESPONSE,
        (service, request) -> service.GetWorkflowExecutionHistoryWithTimeout(request, 1000L),
        ThriftObjects.GET_WORKFLOW_EXECUTION_HISTORY_REQUEST,
        ThriftObjects.GET_WORKFLOW_EXECUTION_HISTORY_RESPONSE);
  }

  @Test
  public void testAsyncGetWorkflowExecutionHistoryWithTimeout() throws Exception {
    testHelperAsync(
        WorkflowAPIGrpc.getGetWorkflowExecutionHistoryMethod(),
        ProtoObjects.GET_WORKFLOW_EXECUTION_HISTORY_REQUEST,
        ProtoObjects.GET_WORKFLOW_EXECUTION_HISTORY_RESPONSE,
        (service, request, handler) ->
            service.GetWorkflowExecutionHistoryWithTimeout(request, handler, 1000L),
        ThriftObjects.GET_WORKFLOW_EXECUTION_HISTORY_REQUEST,
        ThriftObjects.GET_WORKFLOW_EXECUTION_HISTORY_RESPONSE);
  }

  @Test
  public void testIsHealthy() throws Exception {
    stubs.stubResponse(
        MetaAPIGrpc.getHealthMethod(), HealthResponse.newBuilder().setOk(true).build());

    assertTrue(client.isHealthy().join());
  }

  @Test
  public void testAsyncUnsupported() {
    assertUnsupported(WorkflowService.Iface::RestartWorkflowExecution);
    assertUnsupported(WorkflowService.Iface::DiagnoseWorkflowExecution);
    assertUnsupported(WorkflowService.Iface::GetTaskListsByDomain);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RegisterDomain);
    assertAsyncUnsupported(WorkflowService.AsyncIface::DescribeDomain);
    assertAsyncUnsupported(WorkflowService.AsyncIface::DiagnoseWorkflowExecution);
    assertAsyncUnsupported(WorkflowService.AsyncIface::ListDomains);
    assertAsyncUnsupported(WorkflowService.AsyncIface::UpdateDomain);
    assertAsyncUnsupported(WorkflowService.AsyncIface::DeprecateDomain);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RestartWorkflowExecution);
    assertAsyncUnsupported(WorkflowService.AsyncIface::StartWorkflowExecution);
    assertAsyncUnsupported(WorkflowService.AsyncIface::GetWorkflowExecutionHistory);
    assertAsyncUnsupported(WorkflowService.AsyncIface::PollForDecisionTask);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RespondDecisionTaskCompleted);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RespondDecisionTaskFailed);
    assertAsyncUnsupported(WorkflowService.AsyncIface::PollForActivityTask);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RecordActivityTaskHeartbeat);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RecordActivityTaskHeartbeatByID);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RespondActivityTaskCompleted);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RespondActivityTaskCompletedByID);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RespondActivityTaskFailed);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RespondActivityTaskFailedByID);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RespondActivityTaskCanceled);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RespondActivityTaskCanceledByID);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RequestCancelWorkflowExecution);
    assertAsyncUnsupported(WorkflowService.AsyncIface::SignalWithStartWorkflowExecution);
    assertAsyncUnsupported(WorkflowService.AsyncIface::SignalWithStartWorkflowExecutionAsync);
    assertAsyncUnsupported(WorkflowService.AsyncIface::ResetWorkflowExecution);
    assertAsyncUnsupported(WorkflowService.AsyncIface::TerminateWorkflowExecution);
    assertAsyncUnsupported(WorkflowService.AsyncIface::ListOpenWorkflowExecutions);
    assertAsyncUnsupported(WorkflowService.AsyncIface::ListClosedWorkflowExecutions);
    assertAsyncUnsupported(WorkflowService.AsyncIface::ListWorkflowExecutions);
    assertAsyncUnsupported(WorkflowService.AsyncIface::ListArchivedWorkflowExecutions);
    assertAsyncUnsupported(WorkflowService.AsyncIface::ScanWorkflowExecutions);
    assertAsyncUnsupported(WorkflowService.AsyncIface::CountWorkflowExecutions);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RespondQueryTaskCompleted);
    assertAsyncUnsupported(WorkflowService.AsyncIface::ResetStickyTaskList);
    assertAsyncUnsupported(WorkflowService.AsyncIface::QueryWorkflow);
    assertAsyncUnsupported(WorkflowService.AsyncIface::DescribeWorkflowExecution);
    assertAsyncUnsupported(WorkflowService.AsyncIface::DescribeTaskList);
    assertAsyncUnsupported(WorkflowService.AsyncIface::GetTaskListsByDomain);
    assertAsyncUnsupported(WorkflowService.AsyncIface::ListTaskListPartitions);
    assertAsyncUnsupported(WorkflowService.AsyncIface::RefreshWorkflowTasks);
  }

  private <REQ, RES> void assertUnsupported(ThriftFunc<REQ, RES> func) {
    assertThrows(UnsupportedOperationException.class, () -> func.call(client, null));
  }

  private <REQ, RES> void assertAsyncUnsupported(ThriftAsyncFunc<REQ, RES> func) {
    assertThrows(UnsupportedOperationException.class, () -> func.call(client, null, null));
  }

  private <PREQ, PRES, TREQ, TRES> void testHelperAsync(
      MethodDescriptor<PREQ, PRES> method,
      PREQ protoRequest,
      PRES protoResponse,
      ThriftAsyncFunc<TREQ, TRES> clientMethod,
      TREQ thriftRequest,
      TRES thriftResponse)
      throws Exception {
    CompletableFuture<PREQ> protoRequestFuture = stub(method, protoResponse);
    ThriftResponseCallback<TRES> actualResponse = new ThriftResponseCallback<>();

    clientMethod.call(client, thriftRequest, actualResponse);

    assertEquals(
        "request for " + method.getFullMethodName(), protoRequest, protoRequestFuture.join());
    assertEquals(
        "response from " + method.getFullMethodName(), thriftResponse, actualResponse.get());

    stubWithAccessDenied(method);
    ThriftResponseCallback<TRES> errorResponse = new ThriftResponseCallback<>();
    clientMethod.call(client, thriftRequest, errorResponse);
    try {
      errorResponse.get();
      fail("expected exception");
    } catch (CompletionException ex) {
      assertEquals(AccessDeniedError.class, ex.getCause().getClass());
    }
  }

  private <PREQ, PRES, TREQ, TRES> void testHelper(
      MethodDescriptor<PREQ, PRES> method,
      PREQ protoRequest,
      PRES protoResponse,
      ThriftFunc<TREQ, TRES> clientMethod,
      TREQ thriftRequest,
      TRES thriftResponse)
      throws Exception {
    CompletableFuture<PREQ> protoRequestFuture = stub(method, protoResponse);

    TRES actualResponse = clientMethod.call(client, thriftRequest);

    assertEquals(
        "request for " + method.getFullMethodName(), protoRequest, protoRequestFuture.join());
    assertEquals("response from " + method.getFullMethodName(), thriftResponse, actualResponse);

    stubWithAccessDenied(method);
    assertThrows(AccessDeniedError.class, () -> clientMethod.call(client, thriftRequest));
  }

  private <PREQ, PRES, TREQ> void testHelper(
      MethodDescriptor<PREQ, PRES> method,
      PREQ protoRequest,
      PRES protoResponse,
      ThriftCallable<TREQ> clientMethod,
      TREQ thriftRequest)
      throws Exception {
    CompletableFuture<PREQ> protoRequestFuture = stub(method, protoResponse);

    clientMethod.call(client, thriftRequest);

    assertEquals(
        "request for " + method.getFullMethodName(), protoRequest, protoRequestFuture.join());

    stubWithAccessDenied(method);
    assertThrows(AccessDeniedError.class, () -> clientMethod.call(client, thriftRequest));
  }

  private <PREQ, PRES, TRES> void testHelper(
      MethodDescriptor<PREQ, PRES> method,
      PREQ protoRequest,
      PRES protoResponse,
      ThriftProvider<TRES> clientMethod,
      TRES thriftResponse)
      throws Exception {
    CompletableFuture<PREQ> protoRequestFuture = stub(method, protoResponse);

    TRES actualResponse = clientMethod.call(client);

    assertEquals(
        "request for " + method.getFullMethodName(), protoRequest, protoRequestFuture.join());
    assertEquals("response from " + method.getFullMethodName(), thriftResponse, actualResponse);

    stubWithAccessDenied(method);
    assertThrows(AccessDeniedError.class, () -> clientMethod.call(client));
  }

  private void assertTracingHeaders(Header header) {
    assertEquals(1, tracer.finishedSpans().size());
    MockSpan mockSpan = tracer.finishedSpans().get(0);
    assertEquals(
        mockSpan.context().toTraceId(),
        Charsets.UTF_8
            .decode(header.getFieldsMap().get("traceid").getData().asReadOnlyByteBuffer())
            .toString());
    assertEquals(
        mockSpan.context().toSpanId(),
        Charsets.UTF_8
            .decode(header.getFieldsMap().get("spanid").getData().asReadOnlyByteBuffer())
            .toString());
  }

  private <REQ, RES> CompletableFuture<REQ> stub(MethodDescriptor<REQ, RES> method, RES result) {
    return stubs.stubResponse(method, result);
  }

  private <REQ, RES> CompletableFuture<REQ> stubWithAccessDenied(
      MethodDescriptor<REQ, RES> method) {
    return stubs.stubError(method, GRPC_ACCESS_DENIED);
  }

  private interface ThriftProvider<RES> {
    RES call(IWorkflowService service) throws TException;
  }

  private interface ThriftCallable<REQ> {
    void call(IWorkflowService service, REQ req) throws TException;
  }

  private interface ThriftFunc<REQ, RES> {
    RES call(IWorkflowService service, REQ req) throws TException;
  }

  private interface ThriftAsyncFunc<REQ, RES> {
    void call(IWorkflowService service, REQ req, AsyncMethodCallback<RES> callback)
        throws TException;
  }

  private interface StubbedBehavior<REQ, RES> {
    void run(REQ request, StreamObserver<RES> response);
  }

  private static class FakeGrpcServer {
    private final Map<String, Queue<StubbedBehavior<?, ?>>> stubs = new ConcurrentHashMap<>();

    public <REQ, RES> CompletableFuture<REQ> stubResponse(
        MethodDescriptor<REQ, RES> method, RES response) {
      CompletableFuture<REQ> requestFuture = new CompletableFuture<>();
      stub(
          method,
          (req, stream) -> {
            stream.onNext(response);
            stream.onCompleted();
            requestFuture.complete(req);
          });
      return requestFuture;
    }

    public <REQ, RES> CompletableFuture<REQ> stubError(
        MethodDescriptor<REQ, RES> method, StatusRuntimeException exception) {
      CompletableFuture<REQ> requestFuture = new CompletableFuture<>();
      stub(
          method,
          (req, stream) -> {
            stream.onError(exception);
            requestFuture.complete(req);
          });
      return requestFuture;
    }

    public <REQ, RES> void stub(
        MethodDescriptor<REQ, RES> method, StubbedBehavior<REQ, RES> handler) {
      stubs
          .computeIfAbsent(method.getFullMethodName(), (key) -> new ConcurrentLinkedQueue<>())
          .add(handler);
    }

    public Server createServer(ServiceDescriptor... descriptors) {
      try {
        InProcessServerBuilder serverBuilder =
            InProcessServerBuilder.forName("test").directExecutor();
        for (ServiceDescriptor descriptor : descriptors) {
          ServerServiceDefinition.Builder serviceDefinition =
              ServerServiceDefinition.builder(descriptor.getName());
          for (MethodDescriptor<?, ?> method : descriptor.getMethods()) {
            serviceDefinition.addMethod(
                method,
                ServerCalls.asyncUnaryCall(
                    (request, responseObserver) ->
                        handleRequest(method, request, responseObserver)));
          }
          serverBuilder.addService(
              ServerInterceptors.intercept(
                  serviceDefinition.build(), new AuthHeaderValidatingInterceptor()));
        }
        return serverBuilder.build().start();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public ManagedChannel createClient() {
      return InProcessChannelBuilder.forName("test").directExecutor().build();
    }

    public void resetStubs() {
      stubs.clear();
    }

    private void handleRequest(
        MethodDescriptor<?, ?> method, Object request, StreamObserver<?> response) {
      Queue<StubbedBehavior<?, ?>> queue = stubs.get(method.getFullMethodName());
      if (queue == null) {
        throw new IllegalStateException("No behavior stubbed for " + method.getFullMethodName());
      }
      StubbedBehavior<?, ?> behavior = queue.poll();
      if (behavior == null) {
        throw new IllegalStateException(
            "No remaining calls stubbed for " + method.getFullMethodName());
      }
      //noinspection unchecked,rawtypes
      ((StubbedBehavior) behavior).run(request, response);
    }
  }

  private static class AuthHeaderValidatingInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
      if (!headers.containsKey(AUTHORIZATION_HEADER_KEY)) {
        call.close(Status.INVALID_ARGUMENT, new Metadata());
      }
      return next.startCall(call, headers);
    }
  }

  private static class ThriftResponseCallback<T> implements AsyncMethodCallback<T> {
    private final CompletableFuture<T> future = new CompletableFuture<>();

    @Override
    public void onComplete(T response) {
      future.complete(response);
    }

    @Override
    public void onError(Exception exception) {
      future.completeExceptionally(exception);
    }

    public T get() {
      return future.join();
    }
  }
}
