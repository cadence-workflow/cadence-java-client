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

package com.uber.cadence.serviceclient;

import static com.uber.cadence.internal.metrics.MetricsTagValue.REQUEST_TYPE_LONG_POLL;
import static com.uber.cadence.internal.metrics.MetricsTagValue.REQUEST_TYPE_NORMAL;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uber.cadence.*;
import com.uber.cadence.WorkflowService.GetWorkflowExecutionHistory_result;
import com.uber.cadence.internal.Version;
import com.uber.cadence.internal.common.CheckedExceptionWrapper;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.metrics.ServiceMethod;
import com.uber.cadence.internal.tracing.TracingPropagator;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.Stopwatch;
import com.uber.tchannel.api.ResponseCode;
import com.uber.tchannel.api.SubChannel;
import com.uber.tchannel.api.TChannel;
import com.uber.tchannel.api.TFuture;
import com.uber.tchannel.api.errors.TChannelError;
import com.uber.tchannel.errors.ErrorType;
import com.uber.tchannel.messages.ThriftRequest;
import com.uber.tchannel.messages.ThriftResponse;
import com.uber.tchannel.messages.generated.Meta;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowServiceTChannel implements IWorkflowService {
  private static final Logger log = LoggerFactory.getLogger(WorkflowServiceTChannel.class);

  private static final String INTERFACE_NAME = "WorkflowService";

  private final ClientOptions options;
  private final Map<String, String> thriftHeaders;
  private final TChannel tChannel;
  private final TracingPropagator tracingPropagator;
  private final Tracer tracer;
  private final SubChannel subChannel;

  /**
   * Creates Cadence client that connects to the specified host and port using specified options.
   *
   * @param options configuration options like rpc timeouts.
   */
  public WorkflowServiceTChannel(ClientOptions options) {
    this.options = options;
    this.thriftHeaders = getThriftHeaders(options);
    this.tChannel = new TChannel.Builder(options.getClientAppName()).build();
    this.tracingPropagator = new TracingPropagator(options.getTracer());
    this.tracer = options.getTracer();

    InetAddress address;
    try {
      address = InetAddress.getByName(options.getHost());
    } catch (UnknownHostException e) {
      tChannel.shutdown();
      throw new RuntimeException("Unable to get name of host " + options.getHost(), e);
    }

    ArrayList<InetSocketAddress> peers = new ArrayList<>();
    peers.add(new InetSocketAddress(address, options.getPort()));
    this.subChannel = tChannel.makeSubChannel(options.getServiceName()).setPeers(peers);
    log.info(
        "Initialized TChannel for service "
            + this.subChannel.getServiceName()
            + ", LibraryVersion: "
            + Version.LIBRARY_VERSION
            + ", FeatureVersion: "
            + Version.FEATURE_VERSION);
  }

  public void resetSubchannelPeers() throws UnknownHostException {
    InetAddress address = InetAddress.getByName(options.getHost());
    ArrayList<InetSocketAddress> peers = new ArrayList<>();
    peers.add(new InetSocketAddress(address, options.getPort()));
    this.subChannel.setPeers(peers);
  }

  /**
   * Creates Cadence client with specified sub channel and options.
   *
   * @param subChannel sub channel for communicating with cadence frontend service.
   * @param options configuration options like rpc timeouts.
   */
  public WorkflowServiceTChannel(SubChannel subChannel, ClientOptions options) {
    this.options = options;
    this.thriftHeaders = getThriftHeaders(options);
    this.tChannel = null;
    this.subChannel = subChannel;
    this.tracingPropagator = new TracingPropagator(options.getTracer());
    this.tracer = options.getTracer();
  }

  private static Map<String, String> getThriftHeaders(ClientOptions options) {
    String envUserName = System.getProperty("user.name");
    String envHostname;
    try {
      envHostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      envHostname = "localhost";
    }

    ImmutableMap.Builder<String, String> builder =
        ImmutableMap.<String, String>builder()
            .put("user-name", envUserName)
            .put("host-name", envHostname)
            .put("cadence-client-library-version", Version.LIBRARY_VERSION)
            .put("cadence-client-feature-version", Version.FEATURE_VERSION)
            .put("cadence-client-name", "uber-java");

    if (options.getHeaders() != null) {
      for (Map.Entry<String, String> entry : options.getHeaders().entrySet()) {
        builder.put(entry.getKey(), entry.getValue());
      }
    }

    if (options.getFeatureFlags() != null) {
      GsonBuilder gsonBuilder = new GsonBuilder();
      Gson gson = gsonBuilder.create();
      String serialized = gson.toJson(options.getFeatureFlags());
      builder.put("cadence-client-feature-flags", serialized);
    }

    if (!Strings.isNullOrEmpty(options.getIsolationGroup())) {
      builder.put("cadence-client-isolation-group", options.getIsolationGroup());
    }

    return builder.build();
  }

  /** Returns the endpoint in the format service::method" */
  private static String getEndpoint(String service, String method) {
    return String.format("%s::%s", service, method);
  }

  private <T> ThriftRequest<T> buildThriftRequest(String apiName, T body) {
    return buildThriftRequest(apiName, body, null);
  }

  @Override
  public ClientOptions getOptions() {
    return options;
  }

  /**
   * Checks if we have a valid connection to the Cadence cluster, and potentially resets the peer
   * list
   */
  @Override
  public CompletableFuture<Boolean> isHealthy() {
    final ThriftRequest<Meta.health_args> req =
        new ThriftRequest.Builder<Meta.health_args>(options.getServiceName(), "Meta::health")
            .setBody(new Meta.health_args())
            .build();
    final CompletableFuture<Boolean> result = new CompletableFuture<>();
    try {

      final TFuture<ThriftResponse<Meta.health_result>> future = this.subChannel.send(req);
      future.addCallback(
          response -> {
            req.releaseQuietly();
            if (response.isError()) {
              try {
                this.resetSubchannelPeers();
              } catch (final Exception inner_e) {
              }
              result.completeExceptionally(new TException("Rpc error:" + response.getError()));
            } else {
              result.complete(response.getBody(Meta.health_result.class).getSuccess().isOk());
            }
            try {
              response.release();
            } catch (final Exception e) {
              // ignore
            }
          });
    } catch (final TChannelError e) {
      req.releaseQuietly();
      try {
        this.resetSubchannelPeers();
      } catch (final Exception inner_e) {
      }
      result.complete(Boolean.FALSE);
    }
    return result;
  }

  protected <T> ThriftRequest<T> buildThriftRequest(
      String apiName, T body, Long rpcTimeoutOverride) {
    String endpoint = getEndpoint(INTERFACE_NAME, apiName);
    ThriftRequest.Builder<T> builder =
        new ThriftRequest.Builder<>(options.getServiceName(), endpoint);
    // Create a mutable hashmap for headers, as tchannel.tracing.PrefixedHeadersCarrier assumes
    // that it can call put directly to add new stuffs (e.g. traces).
    final HashMap<String, String> headers = new HashMap<>(thriftHeaders);
    TextMapPropagator textMapPropagator =
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

    String tracingHeadersPrefix = "$tracing$";
    TextMapSetter<Map<String, String>> setter =
        (carrier, key, value) -> {
          if (carrier != null) {
            carrier.put(tracingHeadersPrefix + key, value);
          }
        };

    textMapPropagator.inject(Context.current(), headers, setter);

    if (this.options.getAuthProvider() != null) {
      headers.put(
          "cadence-authorization",
          new String(options.getAuthProvider().getAuthToken(), StandardCharsets.UTF_8));
    }
    builder.setHeaders(headers);

    if (rpcTimeoutOverride != null) {
      builder.setTimeout(rpcTimeoutOverride);
    } else {
      builder.setTimeout(this.options.getRpcTimeoutMillis());
    }
    for (Map.Entry<String, String> header : this.options.getTransportHeaders().entrySet()) {
      builder.setTransportHeader(header.getKey(), header.getValue());
    }
    builder.setBody(body);
    return builder.build();
  }

  private <T> ThriftResponse<T> doRemoteCall(ThriftRequest<?> request) throws TException {
    ThriftResponse<T> response = null;
    try {
      TFuture<ThriftResponse<T>> future = subChannel.send(request);
      response = future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TException(e);
    } catch (ExecutionException e) {
      throw new TException(e);
    } catch (TChannelError e) {
      throw new TException("Rpc error", e);
    }
    this.throwOnRpcError(response);
    return response;
  }

  private <T> CompletableFuture<ThriftResponse<T>> doRemoteCallAsync(ThriftRequest<?> request) {
    final CompletableFuture<ThriftResponse<T>> result = new CompletableFuture<>();
    TFuture<ThriftResponse<T>> future = null;
    try {
      future = subChannel.send(request);
    } catch (TChannelError tChannelError) {
      result.completeExceptionally(new TException(tChannelError));
    }
    future.addCallback(
        response -> {
          if (response.isError()) {
            result.completeExceptionally(new TException("Rpc error:" + response.getError()));
          } else {
            result.complete(response);
          }
        });
    return result;
  }

  private void throwOnRpcError(ThriftResponse<?> response) throws TException {
    if (response.isError()) {
      if (response.getError().getErrorType() == ErrorType.Timeout) {
        throw new TTransportException(
            TTransportException.TIMED_OUT, response.getError().getMessage());
      } else {
        throw new TException("Rpc error:" + response.getError());
      }
    }
  }

  @Override
  public void close() {
    if (tChannel != null) {
      tChannel.shutdown();
    }
  }

  interface RemoteCall<T> {
    T apply() throws TException;
  }

  private <T> T measureRemoteCall(String scopeName, RemoteCall<T> call) throws TException {
    return measureRemoteCallWithTags(scopeName, call, null);
  }

  private <T> T measureRemoteCallWithTags(
      String scopeName, RemoteCall<T> call, Map<String, String> tags) throws TException {
    Scope scope = options.getMetricsScope().subScope(scopeName);
    if (tags != null) {
      scope = scope.tagged(tags);
    }
    scope.counter(MetricsType.CADENCE_REQUEST).inc(1);
    Stopwatch sw = scope.timer(MetricsType.CADENCE_LATENCY).start();

    Span span = tracingPropagator.spanByServiceMethod(scopeName);
    try (io.opentracing.Scope tracingScope = tracer.activateSpan(span)) {
      T resp = call.apply();
      sw.stop();
      return resp;
    } catch (EntityNotExistsError
        | WorkflowExecutionAlreadyCompletedError
        | BadRequestError
        | DomainAlreadyExistsError
        | WorkflowExecutionAlreadyStartedError
        | QueryFailedError e) {
      sw.stop();
      scope.counter(MetricsType.CADENCE_INVALID_REQUEST).inc(1);
      throw e;
    } catch (TException e) {
      sw.stop();
      scope.counter(MetricsType.CADENCE_ERROR).inc(1);
      throw e;
    } finally {
      span.finish();
    }
  }

  interface RemoteProc {
    void apply() throws TException;
  }

  private void measureRemoteProc(String scopeName, RemoteProc proc) throws TException {
    measureRemoteCall(
        scopeName,
        () -> {
          proc.apply();
          return null;
        });
  }

  @Override
  public void RegisterDomain(RegisterDomainRequest request) throws TException {
    measureRemoteProc(ServiceMethod.REGISTER_DOMAIN, () -> registerDomain(request));
  }

  private void registerDomain(RegisterDomainRequest registerRequest) throws TException {
    ThriftResponse<WorkflowService.RegisterDomain_result> response = null;
    try {
      ThriftRequest<WorkflowService.RegisterDomain_args> request =
          buildThriftRequest(
              "RegisterDomain", new WorkflowService.RegisterDomain_args(registerRequest));
      response = doRemoteCall(request);
      WorkflowService.RegisterDomain_result result =
          response.getBody(WorkflowService.RegisterDomain_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetDomainExistsError()) {
        throw result.getDomainExistsError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      throw new TException("RegisterDomain failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public DescribeDomainResponse DescribeDomain(DescribeDomainRequest describeRequest)
      throws TException {
    return measureRemoteCall(ServiceMethod.DESCRIBE_DOMAIN, () -> describeDomain(describeRequest));
  }

  private DescribeDomainResponse describeDomain(DescribeDomainRequest describeRequest)
      throws TException {
    ThriftResponse<WorkflowService.DescribeDomain_result> response = null;
    try {
      ThriftRequest<WorkflowService.DescribeDomain_args> request =
          buildThriftRequest(
              "DescribeDomain", new WorkflowService.DescribeDomain_args(describeRequest));
      response = doRemoteCall(request);
      WorkflowService.DescribeDomain_result result =
          response.getBody(WorkflowService.DescribeDomain_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      throw new TException("DescribeDomain failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public DiagnoseWorkflowExecutionResponse DiagnoseWorkflowExecution(
      DiagnoseWorkflowExecutionRequest diagnoseRequest)
      throws DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
          ClientVersionNotSupportedError, TException {
    throw new UnsupportedOperationException("DiagnoseWorkflowExecution is not implemented");
  }

  @Override
  public ListDomainsResponse ListDomains(ListDomainsRequest listRequest)
      throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
          TException {
    return measureRemoteCall(ServiceMethod.LIST_DOMAINS, () -> listDomains(listRequest));
  }

  private ListDomainsResponse listDomains(ListDomainsRequest describeRequest) throws TException {
    ThriftResponse<WorkflowService.ListDomains_result> response = null;
    try {
      ThriftRequest<WorkflowService.ListDomains_args> request =
          buildThriftRequest("ListDomains", new WorkflowService.ListDomains_args(describeRequest));
      response = doRemoteCall(request);
      WorkflowService.ListDomains_result result =
          response.getBody(WorkflowService.ListDomains_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      throw new TException("ListDomains failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public UpdateDomainResponse UpdateDomain(UpdateDomainRequest updateRequest) throws TException {
    return measureRemoteCall(ServiceMethod.UPDATE_DOMAIN, () -> updateDomain(updateRequest));
  }

  private UpdateDomainResponse updateDomain(UpdateDomainRequest updateRequest) throws TException {
    ThriftResponse<WorkflowService.UpdateDomain_result> response = null;
    try {
      ThriftRequest<WorkflowService.UpdateDomain_args> request =
          buildThriftRequest("UpdateDomain", new WorkflowService.UpdateDomain_args(updateRequest));
      response = doRemoteCall(request);
      WorkflowService.UpdateDomain_result result =
          response.getBody(WorkflowService.UpdateDomain_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("UpdateDomain failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void DeprecateDomain(DeprecateDomainRequest deprecateRequest) throws TException {
    measureRemoteProc(ServiceMethod.DEPRECATE_DOMAIN, () -> deprecateDomain(deprecateRequest));
  }

  @Override
  public RestartWorkflowExecutionResponse RestartWorkflowExecution(
      RestartWorkflowExecutionRequest restartRequest)
      throws BadRequestError, ServiceBusyError, DomainNotActiveError, LimitExceededError,
          EntityNotExistsError, ClientVersionNotSupportedError, TException {
    throw new UnsupportedOperationException("unimplemented");
  }

  private void deprecateDomain(DeprecateDomainRequest deprecateRequest) throws TException {
    ThriftResponse<WorkflowService.DeprecateDomain_result> response = null;
    try {
      ThriftRequest<WorkflowService.DeprecateDomain_args> request =
          buildThriftRequest(
              "DeprecateDomain", new WorkflowService.DeprecateDomain_args(deprecateRequest));
      response = doRemoteCall(request);
      WorkflowService.DeprecateDomain_result result =
          response.getBody(WorkflowService.DeprecateDomain_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("DeprecateDomain failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public GetTaskListsByDomainResponse GetTaskListsByDomain(
      GetTaskListsByDomainRequest getTaskListsByDomainRequest) throws TException {
    return measureRemoteCall(
        ServiceMethod.GET_TASK_LISTS_BY_DOMAIN,
        () -> getTaskListsByDomain(getTaskListsByDomainRequest));
  }

  private GetTaskListsByDomainResponse getTaskListsByDomain(
      GetTaskListsByDomainRequest getTaskListsByDomainRequest) throws TException {
    ThriftResponse<WorkflowService.GetTaskListsByDomain_result> response = null;
    try {
      ThriftRequest<WorkflowService.GetTaskListsByDomain_args> request =
          buildThriftRequest(
              "GetTaskListsByDomain",
              new WorkflowService.GetTaskListsByDomain_args(getTaskListsByDomainRequest));
      response = doRemoteCall(request);
      WorkflowService.GetTaskListsByDomain_result result =
          response.getBody(WorkflowService.GetTaskListsByDomain_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("GetTaskListsByDomain failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public StartWorkflowExecutionResponse StartWorkflowExecution(
      StartWorkflowExecutionRequest request) throws TException {
    return measureRemoteCall(
        ServiceMethod.START_WORKFLOW_EXECUTION, () -> startWorkflowExecution(request));
  }

  private StartWorkflowExecutionResponse startWorkflowExecution(
      StartWorkflowExecutionRequest startRequest) throws TException {
    ThriftResponse<WorkflowService.StartWorkflowExecution_result> response = null;
    try {
      initializeStartWorkflowRequest(startRequest);

      ThriftRequest<WorkflowService.StartWorkflowExecution_args> request =
          buildThriftRequest(
              "StartWorkflowExecution",
              new WorkflowService.StartWorkflowExecution_args(startRequest));

      response = doRemoteCall(request);
      WorkflowService.StartWorkflowExecution_result result =
          response.getBody(WorkflowService.StartWorkflowExecution_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetSessionAlreadyExistError()) {
        throw result.getSessionAlreadyExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("StartWorkflowExecution failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public StartWorkflowExecutionAsyncResponse StartWorkflowExecutionAsync(
      StartWorkflowExecutionAsyncRequest startAsyncRequest) throws TException {
    return measureRemoteCall(
        ServiceMethod.START_WORKFLOW_EXECUTION_ASYNC,
        () -> startWorkflowExecutionAsync(startAsyncRequest));
  }

  private StartWorkflowExecutionAsyncResponse startWorkflowExecutionAsync(
      StartWorkflowExecutionAsyncRequest startAsyncRequest) throws TException {
    ThriftResponse<WorkflowService.StartWorkflowExecutionAsync_result> response = null;
    try {
      initializeStartWorkflowRequest(startAsyncRequest.getRequest());

      ThriftRequest<WorkflowService.StartWorkflowExecutionAsync_args> request =
          buildThriftRequest(
              "StartWorkflowExecutionAsync",
              new WorkflowService.StartWorkflowExecutionAsync_args(startAsyncRequest));

      response = doRemoteCall(request);
      WorkflowService.StartWorkflowExecutionAsync_result result =
          response.getBody(WorkflowService.StartWorkflowExecutionAsync_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetSessionAlreadyExistError()) {
        throw result.getSessionAlreadyExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("StartWorkflowExecution failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  private void initializeStartWorkflowRequest(StartWorkflowExecutionRequest startRequest) {
    if (!startRequest.isSetRequestId()) {
      startRequest.setRequestId(UUID.randomUUID().toString());
    }
    // Write span context to header
    if (!startRequest.isSetHeader()) {
      startRequest.setHeader(new Header());
    }
    tracingPropagator.inject(startRequest.getHeader());
  }

  @Override
  public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest request, Long timeoutInMillis) throws TException {
    Map<String, String> tags =
        ImmutableMap.of(
            MetricsTag.REQUEST_TYPE,
            request.isWaitForNewEvent() ? REQUEST_TYPE_LONG_POLL : REQUEST_TYPE_NORMAL);
    return measureRemoteCallWithTags(
        ServiceMethod.GET_WORKFLOW_EXECUTION_HISTORY,
        () -> getWorkflowExecutionHistory(request, timeoutInMillis),
        tags);
  }

  @Override
  public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest request) throws TException {
    Map<String, String> tags =
        ImmutableMap.of(
            MetricsTag.REQUEST_TYPE,
            request.isWaitForNewEvent() ? REQUEST_TYPE_LONG_POLL : REQUEST_TYPE_NORMAL);
    return measureRemoteCallWithTags(
        ServiceMethod.GET_WORKFLOW_EXECUTION_HISTORY,
        () -> getWorkflowExecutionHistory(request, null),
        tags);
  }

  private GetWorkflowExecutionHistoryResponse getWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis) throws TException {
    ThriftResponse<WorkflowService.GetWorkflowExecutionHistory_result> response = null;
    try {
      ThriftRequest<WorkflowService.GetWorkflowExecutionHistory_args> request =
          buildGetWorkflowExecutionHistoryThriftRequest(getRequest, timeoutInMillis);
      response = doRemoteCall(request);
      WorkflowService.GetWorkflowExecutionHistory_result result =
          response.getBody(WorkflowService.GetWorkflowExecutionHistory_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        GetWorkflowExecutionHistoryResponse res = result.getSuccess();
        if (res.getRawHistory() != null) {
          History history =
              InternalUtils.DeserializeFromBlobDataToHistory(
                  res.getRawHistory(), getRequest.getHistoryEventFilterType());
          res.setHistory(history);
        }
        return res;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("GetWorkflowExecutionHistory failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  private ThriftRequest<WorkflowService.GetWorkflowExecutionHistory_args>
      buildGetWorkflowExecutionHistoryThriftRequest(
          GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis) {

    if (getRequest.isWaitForNewEvent()) {
      timeoutInMillis =
          validateAndUpdateTimeout(timeoutInMillis, options.getRpcLongPollTimeoutMillis());
    } else {
      timeoutInMillis = validateAndUpdateTimeout(timeoutInMillis, options.getRpcTimeoutMillis());
    }

    return buildThriftRequest(
        "GetWorkflowExecutionHistory",
        new WorkflowService.GetWorkflowExecutionHistory_args(getRequest),
        timeoutInMillis);
  }

  @Override
  public PollForDecisionTaskResponse PollForDecisionTask(PollForDecisionTaskRequest request)
      throws TException {
    return measureRemoteCall(
        ServiceMethod.POLL_FOR_DECISION_TASK, () -> pollForDecisionTask(request));
  }

  private PollForDecisionTaskResponse pollForDecisionTask(PollForDecisionTaskRequest pollRequest)
      throws TException {
    ThriftResponse<WorkflowService.PollForDecisionTask_result> response = null;
    try {
      ThriftRequest<WorkflowService.PollForDecisionTask_args> request =
          buildThriftRequest(
              "PollForDecisionTask",
              new WorkflowService.PollForDecisionTask_args(pollRequest),
              options.getRpcLongPollTimeoutMillis());
      response = doRemoteCall(request);
      WorkflowService.PollForDecisionTask_result result =
          response.getBody(WorkflowService.PollForDecisionTask_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("PollForDecisionTask failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public RespondDecisionTaskCompletedResponse RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completedRequest) throws TException {
    return measureRemoteCall(
        ServiceMethod.RESPOND_DECISION_TASK_COMPLETED,
        () -> respondDecisionTaskCompleted(completedRequest));
  }

  private RespondDecisionTaskCompletedResponse respondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completedRequest) throws TException {
    ThriftResponse<WorkflowService.RespondDecisionTaskCompleted_result> response = null;
    try {
      ThriftRequest<WorkflowService.RespondDecisionTaskCompleted_args> request =
          buildThriftRequest(
              "RespondDecisionTaskCompleted",
              new WorkflowService.RespondDecisionTaskCompleted_args(completedRequest));
      response = doRemoteCall(request);
      WorkflowService.RespondDecisionTaskCompleted_result result =
          response.getBody(WorkflowService.RespondDecisionTaskCompleted_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RespondDecisionTaskCompleted failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest request)
      throws TException {
    measureRemoteProc(
        ServiceMethod.RESPOND_DECISION_TASK_FAILED, () -> respondDecisionTaskFailed(request));
  }

  private void respondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest)
      throws TException {
    ThriftResponse<WorkflowService.RespondDecisionTaskFailed_result> response = null;
    try {
      ThriftRequest<WorkflowService.RespondDecisionTaskFailed_args> request =
          buildThriftRequest(
              "RespondDecisionTaskFailed",
              new WorkflowService.RespondDecisionTaskFailed_args(failedRequest));
      response = doRemoteCall(request);
      WorkflowService.RespondDecisionTaskFailed_result result =
          response.getBody(WorkflowService.RespondDecisionTaskFailed_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RespondDecisionTaskFailed failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public PollForActivityTaskResponse PollForActivityTask(PollForActivityTaskRequest request)
      throws TException {
    return measureRemoteCall(
        ServiceMethod.POLL_FOR_ACTIVITY_TASK, () -> pollForActivityTask(request));
  }

  private PollForActivityTaskResponse pollForActivityTask(PollForActivityTaskRequest pollRequest)
      throws TException {
    ThriftResponse<WorkflowService.PollForActivityTask_result> response = null;
    try {
      ThriftRequest<WorkflowService.PollForActivityTask_args> request =
          buildThriftRequest(
              "PollForActivityTask",
              new WorkflowService.PollForActivityTask_args(pollRequest),
              options.getRpcLongPollTimeoutMillis());
      response = doRemoteCall(request);
      WorkflowService.PollForActivityTask_result result =
          response.getBody(WorkflowService.PollForActivityTask_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("PollForActivityTask failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest request) throws TException {
    return measureRemoteCall(
        ServiceMethod.RECORD_ACTIVITY_TASK_HEARTBEAT, () -> recordActivityTaskHeartbeat(request));
  }

  private RecordActivityTaskHeartbeatResponse recordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest) throws TException {
    ThriftResponse<WorkflowService.RecordActivityTaskHeartbeat_result> response = null;
    try {
      ThriftRequest<WorkflowService.RecordActivityTaskHeartbeat_args> request =
          buildThriftRequest(
              "RecordActivityTaskHeartbeat",
              new WorkflowService.RecordActivityTaskHeartbeat_args(heartbeatRequest));
      response = doRemoteCall(request);
      WorkflowService.RecordActivityTaskHeartbeat_result result =
          response.getBody(WorkflowService.RecordActivityTaskHeartbeat_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RecordActivityTaskHeartbeat failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest)
      throws BadRequestError, InternalServiceError, EntityNotExistsError, DomainNotActiveError,
          WorkflowExecutionAlreadyCompletedError, LimitExceededError, ServiceBusyError, TException {
    return measureRemoteCall(
        ServiceMethod.RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID,
        () -> recordActivityTaskHeartbeatByID(heartbeatRequest));
  }

  private RecordActivityTaskHeartbeatResponse recordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest) throws TException {
    ThriftResponse<WorkflowService.RecordActivityTaskHeartbeatByID_result> response = null;
    try {
      ThriftRequest<WorkflowService.RecordActivityTaskHeartbeatByID_args> request =
          buildThriftRequest(
              "RecordActivityTaskHeartbeatByID",
              new WorkflowService.RecordActivityTaskHeartbeatByID_args(heartbeatRequest));
      response = doRemoteCall(request);
      WorkflowService.RecordActivityTaskHeartbeatByID_result result =
          response.getBody(WorkflowService.RecordActivityTaskHeartbeatByID_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RecordActivityTaskHeartbeatByID failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest request)
      throws TException {
    measureRemoteProc(
        ServiceMethod.RESPOND_ACTIVITY_TASK_COMPLETED, () -> respondActivityTaskCompleted(request));
  }

  private void respondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest)
      throws TException {
    ThriftResponse<WorkflowService.RespondActivityTaskCompleted_result> response = null;
    try {
      ThriftRequest<WorkflowService.RespondActivityTaskCompleted_args> request =
          buildThriftRequest(
              "RespondActivityTaskCompleted",
              new WorkflowService.RespondActivityTaskCompleted_args(completeRequest));
      response = doRemoteCall(request);
      WorkflowService.RespondActivityTaskCompleted_result result =
          response.getBody(WorkflowService.RespondActivityTaskCompleted_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RespondActivityTaskCompleted failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void RespondActivityTaskCompletedByID(RespondActivityTaskCompletedByIDRequest request)
      throws TException {
    measureRemoteProc(
        ServiceMethod.RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID,
        () -> respondActivityTaskCompletedByID(request));
  }

  private void respondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest) throws TException {
    ThriftResponse<WorkflowService.RespondActivityTaskCompletedByID_result> response = null;
    try {
      ThriftRequest<WorkflowService.RespondActivityTaskCompletedByID_args> request =
          buildThriftRequest(
              "RespondActivityTaskCompletedByID",
              new WorkflowService.RespondActivityTaskCompletedByID_args(completeRequest));
      response = doRemoteCall(request);
      WorkflowService.RespondActivityTaskCompletedByID_result result =
          response.getBody(WorkflowService.RespondActivityTaskCompletedByID_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RespondActivityTaskCompletedByID failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void RespondActivityTaskFailed(RespondActivityTaskFailedRequest request)
      throws TException {
    measureRemoteProc(
        ServiceMethod.RESPOND_ACTIVITY_TASK_FAILED, () -> respondActivityTaskFailed(request));
  }

  private void respondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest)
      throws TException {
    ThriftResponse<WorkflowService.RespondActivityTaskFailed_result> response = null;
    try {
      ThriftRequest<WorkflowService.RespondActivityTaskFailed_args> request =
          buildThriftRequest(
              "RespondActivityTaskFailed",
              new WorkflowService.RespondActivityTaskFailed_args(failRequest));
      response = doRemoteCall(request);
      WorkflowService.RespondActivityTaskFailed_result result =
          response.getBody(WorkflowService.RespondActivityTaskFailed_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RespondActivityTaskFailed failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest request)
      throws TException {
    measureRemoteProc(
        ServiceMethod.RESPOND_ACTIVITY_TASK_FAILED_BY_ID,
        () -> respondActivityTaskFailedByID(request));
  }

  private void respondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest)
      throws TException {
    ThriftResponse<WorkflowService.RespondActivityTaskFailedByID_result> response = null;
    try {
      ThriftRequest<WorkflowService.RespondActivityTaskFailedByID_args> request =
          buildThriftRequest(
              "RespondActivityTaskFailedByID",
              new WorkflowService.RespondActivityTaskFailedByID_args(failRequest));
      response = doRemoteCall(request);
      WorkflowService.RespondActivityTaskFailedByID_result result =
          response.getBody(WorkflowService.RespondActivityTaskFailedByID_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RespondActivityTaskFailedByID failedByID with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest request)
      throws TException {
    measureRemoteProc(
        ServiceMethod.RESPOND_ACTIVITY_TASK_CANCELED, () -> respondActivityTaskCanceled(request));
  }

  private void respondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest)
      throws TException {
    ThriftResponse<WorkflowService.RespondActivityTaskCanceled_result> response = null;
    try {
      ThriftRequest<WorkflowService.RespondActivityTaskCanceled_args> request =
          buildThriftRequest(
              "RespondActivityTaskCanceled",
              new WorkflowService.RespondActivityTaskCanceled_args(canceledRequest));
      response = doRemoteCall(request);
      WorkflowService.RespondActivityTaskCanceled_result result =
          response.getBody(WorkflowService.RespondActivityTaskCanceled_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RespondActivityTaskCanceled failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void RespondActivityTaskCanceledByID(RespondActivityTaskCanceledByIDRequest request)
      throws TException {
    measureRemoteProc(
        ServiceMethod.RESPOND_ACTIVITY_TASK_CANCELED_BY_ID,
        () -> respondActivityTaskCanceledByID(request));
  }

  private void respondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledByIDRequest) throws TException {
    ThriftResponse<WorkflowService.RespondActivityTaskCanceledByID_result> response = null;
    try {
      ThriftRequest<WorkflowService.RespondActivityTaskCanceledByID_args> request =
          buildThriftRequest(
              "RespondActivityTaskCanceledByID",
              new WorkflowService.RespondActivityTaskCanceledByID_args(canceledByIDRequest));
      response = doRemoteCall(request);
      WorkflowService.RespondActivityTaskCanceledByID_result result =
          response.getBody(WorkflowService.RespondActivityTaskCanceledByID_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RespondActivityTaskCanceledByID failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest request)
      throws TException {
    measureRemoteProc(
        ServiceMethod.REQUEST_CANCEL_WORKFLOW_EXECUTION,
        () -> requestCancelWorkflowExecution(request));
  }

  private void requestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest)
      throws TException {
    if (!cancelRequest.isSetRequestId()) {
      cancelRequest.setRequestId(UUID.randomUUID().toString());
    }
    ThriftResponse<WorkflowService.RequestCancelWorkflowExecution_result> response = null;
    try {
      ThriftRequest<WorkflowService.RequestCancelWorkflowExecution_args> request =
          buildThriftRequest(
              "RequestCancelWorkflowExecution",
              new WorkflowService.RequestCancelWorkflowExecution_args(cancelRequest));
      response = doRemoteCall(request);
      WorkflowService.RequestCancelWorkflowExecution_result result =
          response.getBody(WorkflowService.RequestCancelWorkflowExecution_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetCancellationAlreadyRequestedError()) {
        throw result.getCancellationAlreadyRequestedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RequestCancelWorkflowExecution failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void SignalWorkflowExecution(SignalWorkflowExecutionRequest request) throws TException {
    measureRemoteProc(
        ServiceMethod.SIGNAL_WORKFLOW_EXECUTION, () -> signalWorkflowExecution(request));
  }

  private void signalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest)
      throws TException {
    ThriftResponse<WorkflowService.SignalWorkflowExecution_result> response = null;
    try {
      ThriftRequest<WorkflowService.SignalWorkflowExecution_args> request =
          buildThriftRequest(
              "SignalWorkflowExecution",
              new WorkflowService.SignalWorkflowExecution_args(signalRequest));
      response = doRemoteCall(request);
      WorkflowService.SignalWorkflowExecution_result result =
          response.getBody(WorkflowService.SignalWorkflowExecution_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("SignalWorkflowExecution failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public StartWorkflowExecutionResponse SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest) throws TException {
    return measureRemoteCall(
        ServiceMethod.SIGNAL_WITH_START_WORKFLOW_EXECUTION,
        () -> signalWithStartWorkflowExecution(signalWithStartRequest));
  }

  @Override
  public SignalWithStartWorkflowExecutionAsyncResponse SignalWithStartWorkflowExecutionAsync(
      SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest) throws TException {
    return measureRemoteCall(
        ServiceMethod.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC,
        () -> signalWithStartWorkflowExecutionAsync(signalWithStartRequest));
  }

  private SignalWithStartWorkflowExecutionAsyncResponse signalWithStartWorkflowExecutionAsync(
      SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest) throws TException {
    ThriftResponse<WorkflowService.SignalWithStartWorkflowExecutionAsync_result> response = null;
    try {
      initializeSignalWithStartWorkflowRequest(signalWithStartRequest.getRequest());

      ThriftRequest<WorkflowService.SignalWithStartWorkflowExecutionAsync_args> request =
          buildThriftRequest(
              "SignalWithStartWorkflowExecutionAsync",
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_args(
                  signalWithStartRequest));

      response = doRemoteCall(request);
      WorkflowService.SignalWithStartWorkflowExecutionAsync_result result =
          response.getBody(WorkflowService.SignalWithStartWorkflowExecutionAsync_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException(
          "SignalWithStartWorkflowExecutionAsync failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public ResetWorkflowExecutionResponse ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest)
      throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, ClientVersionNotSupportedError, TException {
    return measureRemoteCall(
        ServiceMethod.RESET_WORKFLOW_EXECUTION, () -> resetWorkflowExecution(resetRequest));
  }

  private ResetWorkflowExecutionResponse resetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest) throws TException {
    ThriftResponse<WorkflowService.ResetWorkflowExecution_result> response = null;
    try {
      ThriftRequest<WorkflowService.ResetWorkflowExecution_args> request =
          buildThriftRequest(
              "ResetWorkflowExecution",
              new WorkflowService.ResetWorkflowExecution_args(resetRequest));
      response = doRemoteCall(request);
      WorkflowService.ResetWorkflowExecution_result result =
          response.getBody(WorkflowService.ResetWorkflowExecution_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("ResetWorkflowExecution failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  private StartWorkflowExecutionResponse signalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest) throws TException {
    ThriftResponse<WorkflowService.SignalWithStartWorkflowExecution_result> response = null;
    try {
      initializeSignalWithStartWorkflowRequest(signalWithStartRequest);

      ThriftRequest<WorkflowService.SignalWithStartWorkflowExecution_args> request =
          buildThriftRequest(
              "SignalWithStartWorkflowExecution",
              new WorkflowService.SignalWithStartWorkflowExecution_args(signalWithStartRequest));

      response = doRemoteCall(request);
      WorkflowService.SignalWithStartWorkflowExecution_result result =
          response.getBody(WorkflowService.SignalWithStartWorkflowExecution_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("SignalWithStartWorkflowExecution failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  private void initializeSignalWithStartWorkflowRequest(
      SignalWithStartWorkflowExecutionRequest request) {
    if (!request.isSetRequestId()) {
      request.setRequestId(UUID.randomUUID().toString());
    }
    // Write span context to header
    if (!request.isSetHeader()) {
      request.setHeader(new Header());
    }
    tracingPropagator.inject(request.getHeader());
  }

  @Override
  public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest request)
      throws TException {
    measureRemoteProc(
        ServiceMethod.TERMINATE_WORKFLOW_EXECUTION, () -> terminateWorkflowExecution(request));
  }

  private void terminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest)
      throws TException {
    ThriftResponse<WorkflowService.TerminateWorkflowExecution_result> response = null;
    try {
      ThriftRequest<WorkflowService.TerminateWorkflowExecution_args> request =
          buildThriftRequest(
              "TerminateWorkflowExecution",
              new WorkflowService.TerminateWorkflowExecution_args(terminateRequest));
      response = doRemoteCall(request);
      WorkflowService.TerminateWorkflowExecution_result result =
          response.getBody(WorkflowService.TerminateWorkflowExecution_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("TerminateWorkflowExecution failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public ListOpenWorkflowExecutionsResponse ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest request) throws TException {
    return measureRemoteCall(
        ServiceMethod.LIST_OPEN_WORKFLOW_EXECUTIONS, () -> listOpenWorkflowExecutions(request));
  }

  private ListOpenWorkflowExecutionsResponse listOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest) throws TException {
    ThriftResponse<WorkflowService.ListOpenWorkflowExecutions_result> response = null;
    try {
      ThriftRequest<WorkflowService.ListOpenWorkflowExecutions_args> request =
          buildThriftRequest(
              "ListOpenWorkflowExecutions",
              new WorkflowService.ListOpenWorkflowExecutions_args(listRequest));
      response = doRemoteCall(request);
      WorkflowService.ListOpenWorkflowExecutions_result result =
          response.getBody(WorkflowService.ListOpenWorkflowExecutions_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("ListOpenWorkflowExecutions failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public ListClosedWorkflowExecutionsResponse ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest request) throws TException {
    return measureRemoteCall(
        ServiceMethod.LIST_CLOSED_WORKFLOW_EXECUTIONS, () -> listClosedWorkflowExecutions(request));
  }

  private ListClosedWorkflowExecutionsResponse listClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest) throws TException {
    ThriftResponse<WorkflowService.ListClosedWorkflowExecutions_result> response = null;
    try {
      ThriftRequest<WorkflowService.ListClosedWorkflowExecutions_args> request =
          buildThriftRequest(
              "ListClosedWorkflowExecutions",
              new WorkflowService.ListClosedWorkflowExecutions_args(listRequest));
      response = doRemoteCall(request);
      WorkflowService.ListClosedWorkflowExecutions_result result =
          response.getBody(WorkflowService.ListClosedWorkflowExecutions_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("ListClosedWorkflowExecutions failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public ListWorkflowExecutionsResponse ListWorkflowExecutions(
      ListWorkflowExecutionsRequest request)
      throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return measureRemoteCall(
        ServiceMethod.LIST_WORKFLOW_EXECUTIONS, () -> listWorkflowExecutions(request));
  }

  private ListWorkflowExecutionsResponse listWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest) throws TException {
    ThriftResponse<WorkflowService.ListWorkflowExecutions_result> response = null;
    try {
      ThriftRequest<WorkflowService.ListWorkflowExecutions_args> request =
          buildThriftRequest(
              "ListWorkflowExecutions",
              new WorkflowService.ListWorkflowExecutions_args(listRequest));
      response = doRemoteCall(request);
      WorkflowService.ListWorkflowExecutions_result result =
          response.getBody(WorkflowService.ListWorkflowExecutions_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("ListWorkflowExecutions failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public ListArchivedWorkflowExecutionsResponse ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return measureRemoteCall(
        ServiceMethod.LIST_ARCHIVED_WORKFLOW_EXECUTIONS,
        () -> listArchivedWorkflowExecutions(listRequest));
  }

  private ListArchivedWorkflowExecutionsResponse listArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest) throws TException {
    ThriftResponse<WorkflowService.ListArchivedWorkflowExecutions_result> response = null;
    try {
      ThriftRequest<WorkflowService.ListArchivedWorkflowExecutions_args> request =
          buildThriftRequest(
              "ListArchivedWorkflowExecutions",
              new WorkflowService.ListArchivedWorkflowExecutions_args(listRequest),
              options.getRpcListArchivedWorkflowTimeoutMillis());
      response = doRemoteCall(request);
      WorkflowService.ListArchivedWorkflowExecutions_result result =
          response.getBody(WorkflowService.ListArchivedWorkflowExecutions_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("ListArchivedWorkflowExecutions failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public ListWorkflowExecutionsResponse ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest request)
      throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return measureRemoteCall(
        ServiceMethod.SCAN_WORKFLOW_EXECUTIONS, () -> scanWorkflowExecutions(request));
  }

  private ListWorkflowExecutionsResponse scanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest) throws TException {
    ThriftResponse<WorkflowService.ScanWorkflowExecutions_result> response = null;
    try {
      ThriftRequest<WorkflowService.ScanWorkflowExecutions_args> request =
          buildThriftRequest(
              "ScanWorkflowExecutions",
              new WorkflowService.ScanWorkflowExecutions_args(listRequest));
      response = doRemoteCall(request);
      WorkflowService.ScanWorkflowExecutions_result result =
          response.getBody(WorkflowService.ScanWorkflowExecutions_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("ScanWorkflowExecutions failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public CountWorkflowExecutionsResponse CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest)
      throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, TException {
    return measureRemoteCall(
        ServiceMethod.COUNT_WORKFLOW_EXECUTIONS, () -> countWorkflowExecutions(countRequest));
  }

  private CountWorkflowExecutionsResponse countWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest) throws TException {
    ThriftResponse<WorkflowService.CountWorkflowExecutions_result> response = null;
    try {
      ThriftRequest<WorkflowService.CountWorkflowExecutions_args> request =
          buildThriftRequest(
              "CountWorkflowExecutions",
              new WorkflowService.CountWorkflowExecutions_args(countRequest));
      response = doRemoteCall(request);
      WorkflowService.CountWorkflowExecutions_result result =
          response.getBody(WorkflowService.CountWorkflowExecutions_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("CountWorkflowExecutions failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public GetSearchAttributesResponse GetSearchAttributes()
      throws InternalServiceError, ServiceBusyError, ClientVersionNotSupportedError, TException {
    return measureRemoteCall(ServiceMethod.GET_SEARCH_ATTRIBUTES, () -> getSearchAttributes());
  }

  private GetSearchAttributesResponse getSearchAttributes() throws TException {
    ThriftResponse<WorkflowService.GetSearchAttributes_result> response = null;
    try {
      ThriftRequest<WorkflowService.GetSearchAttributes_args> request =
          buildThriftRequest("GetSearchAttributes", new WorkflowService.GetSearchAttributes_args());
      response = doRemoteCall(request);
      WorkflowService.GetSearchAttributes_result result =
          response.getBody(WorkflowService.GetSearchAttributes_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("GetSearchAttributes failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest request)
      throws TException {
    measureRemoteProc(
        ServiceMethod.RESPOND_QUERY_TASK_COMPLETED, () -> respondQueryTaskCompleted(request));
  }

  private void respondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest)
      throws TException {
    ThriftResponse<WorkflowService.RespondQueryTaskCompleted_result> response = null;
    try {
      ThriftRequest<WorkflowService.RespondQueryTaskCompleted_args> request =
          buildThriftRequest(
              "RespondQueryTaskCompleted",
              new WorkflowService.RespondQueryTaskCompleted_args(completeRequest));
      response = doRemoteCall(request);
      WorkflowService.RespondQueryTaskCompleted_result result =
          response.getBody(WorkflowService.RespondQueryTaskCompleted_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("RespondQueryTaskCompleted failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest request) throws TException {
    return measureRemoteCall(ServiceMethod.QUERY_WORKFLOW, () -> queryWorkflow(request));
  }

  private QueryWorkflowResponse queryWorkflow(QueryWorkflowRequest queryRequest) throws TException {
    ThriftResponse<WorkflowService.QueryWorkflow_result> response = null;
    try {
      ThriftRequest<WorkflowService.QueryWorkflow_args> request =
          buildThriftRequest(
              "QueryWorkflow",
              new WorkflowService.QueryWorkflow_args(queryRequest),
              options.getRpcQueryTimeoutMillis());
      response = doRemoteCall(request);
      WorkflowService.QueryWorkflow_result result =
          response.getBody(WorkflowService.QueryWorkflow_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetQueryFailedError()) {
        throw result.getQueryFailedError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("QueryWorkflow failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public ResetStickyTaskListResponse ResetStickyTaskList(ResetStickyTaskListRequest resetRequest)
      throws BadRequestError, InternalServiceError, EntityNotExistsError, LimitExceededError,
          WorkflowExecutionAlreadyCompletedError, ServiceBusyError, DomainNotActiveError,
          TException {
    return measureRemoteCall(
        ServiceMethod.RESET_STICKY_TASK_LIST, () -> resetStickyTaskList(resetRequest));
  }

  private ResetStickyTaskListResponse resetStickyTaskList(ResetStickyTaskListRequest queryRequest)
      throws TException {
    ThriftResponse<WorkflowService.ResetStickyTaskList_result> response = null;
    try {
      ThriftRequest<WorkflowService.ResetStickyTaskList_args> request =
          buildThriftRequest(
              "ResetStickyTaskList",
              new WorkflowService.ResetStickyTaskList_args(queryRequest),
              options.getRpcQueryTimeoutMillis());
      response = doRemoteCall(request);
      WorkflowService.ResetStickyTaskList_result result =
          response.getBody(WorkflowService.ResetStickyTaskList_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
        throw result.getWorkflowExecutionAlreadyCompletedError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("ResetStickyTaskList failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public DescribeWorkflowExecutionResponse DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest request) throws TException {
    return measureRemoteCall(
        ServiceMethod.DESCRIBE_WORKFLOW_EXECUTION, () -> describeWorkflowExecution(request));
  }

  private DescribeWorkflowExecutionResponse describeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest) throws TException {
    ThriftResponse<WorkflowService.DescribeWorkflowExecution_result> response = null;
    try {
      ThriftRequest<WorkflowService.DescribeWorkflowExecution_args> request =
          buildThriftRequest(
              "DescribeWorkflowExecution",
              new WorkflowService.DescribeWorkflowExecution_args(describeRequest));
      response = doRemoteCall(request);
      WorkflowService.DescribeWorkflowExecution_result result =
          response.getBody(WorkflowService.DescribeWorkflowExecution_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("DescribeWorkflowExecution failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest request)
      throws TException {
    return measureRemoteCall(ServiceMethod.DESCRIBE_TASK_LIST, () -> describeTaskList(request));
  }

  private DescribeTaskListResponse describeTaskList(DescribeTaskListRequest describeRequest)
      throws TException {
    ThriftResponse<WorkflowService.DescribeTaskList_result> response = null;
    try {
      ThriftRequest<WorkflowService.DescribeTaskList_args> request =
          buildThriftRequest(
              "DescribeTaskList", new WorkflowService.DescribeTaskList_args(describeRequest));
      response = doRemoteCall(request);
      WorkflowService.DescribeTaskList_result result =
          response.getBody(WorkflowService.DescribeTaskList_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      if (result.isSetClientVersionNotSupportedError()) {
        throw result.getClientVersionNotSupportedError();
      }
      throw new TException("DescribeTaskList failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public ClusterInfo GetClusterInfo() throws InternalServiceError, ServiceBusyError, TException {
    return measureRemoteCall(ServiceMethod.GET_CLUSTER_INFO, () -> getClusterInfo());
  }

  private ClusterInfo getClusterInfo() throws TException {
    ThriftResponse<WorkflowService.GetClusterInfo_result> response = null;
    try {
      ThriftRequest<WorkflowService.GetClusterInfo_args> request =
          buildThriftRequest("GetClusterInfo", new WorkflowService.GetClusterInfo_args());
      response = doRemoteCall(request);
      WorkflowService.GetClusterInfo_result result =
          response.getBody(WorkflowService.GetClusterInfo_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetInternalServiceError()) {
        throw result.getInternalServiceError();
      }
      throw new TException("GetClusterInfo failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public ListTaskListPartitionsResponse ListTaskListPartitions(
      ListTaskListPartitionsRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          TException {
    return measureRemoteCall(
        ServiceMethod.LIST_TASK_LIST_PARTITIONS, () -> listTaskListPartitions(request));
  }

  @Override
  public void RefreshWorkflowTasks(RefreshWorkflowTasksRequest refreshWorkflowTasks)
      throws BadRequestError, DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
          TException {
    ThriftResponse<WorkflowService.RefreshWorkflowTasks_result> response = null;
    try {
      ThriftRequest<WorkflowService.RefreshWorkflowTasks_args> request =
          buildThriftRequest(
              "RefreshWorkflowTasks",
              new WorkflowService.RefreshWorkflowTasks_args(refreshWorkflowTasks));
      response = doRemoteCall(request);
      WorkflowService.RefreshWorkflowTasks_result result =
          response.getBody(WorkflowService.RefreshWorkflowTasks_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return;
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetDomainNotActiveError()) {
        throw result.getDomainNotActiveError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      throw new TException("RefreshWorkflowTasks failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  private ListTaskListPartitionsResponse listTaskListPartitions(
      ListTaskListPartitionsRequest listRequest) throws TException {
    ThriftResponse<WorkflowService.ListTaskListPartitions_result> response = null;
    try {
      ThriftRequest<WorkflowService.ListTaskListPartitions_args> request =
          buildThriftRequest(
              "ListTaskListPartitions",
              new WorkflowService.ListTaskListPartitions_args(listRequest));
      response = doRemoteCall(request);
      WorkflowService.ListTaskListPartitions_result result =
          response.getBody(WorkflowService.ListTaskListPartitions_result.class);
      if (response.getResponseCode() == ResponseCode.OK) {
        return result.getSuccess();
      }
      if (result.isSetBadRequestError()) {
        throw result.getBadRequestError();
      }
      if (result.isSetEntityNotExistError()) {
        throw result.getEntityNotExistError();
      }
      if (result.isSetServiceBusyError()) {
        throw result.getServiceBusyError();
      }
      if (result.isSetLimitExceededError()) {
        throw result.getLimitExceededError();
      }
      throw new TException("ListTaskListPartitions failed with unknown error:" + result);
    } finally {
      if (response != null) {
        response.release();
      }
    }
  }

  @Override
  public void StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest, AsyncMethodCallback resultHandler) {
    startWorkflowExecution(startRequest, resultHandler, null);
  }

  @Override
  public void StartWorkflowExecutionWithTimeout(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis) {
    startWorkflowExecution(startRequest, resultHandler, timeoutInMillis);
  }

  private void startWorkflowExecution(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis) {
    initializeStartWorkflowRequest(startRequest);
    timeoutInMillis = validateAndUpdateTimeout(timeoutInMillis, options.getRpcTimeoutMillis());
    ThriftRequest<WorkflowService.StartWorkflowExecution_args> request =
        buildThriftRequest(
            "StartWorkflowExecution",
            new WorkflowService.StartWorkflowExecution_args(startRequest),
            timeoutInMillis);

    CompletableFuture<ThriftResponse<WorkflowService.StartWorkflowExecution_result>> response =
        doRemoteCallAsync(request);
    response
        .whenComplete(
            (r, e) -> {
              try {
                if (e != null) {
                  resultHandler.onError(CheckedExceptionWrapper.wrap(e));
                  return;
                }
                WorkflowService.StartWorkflowExecution_result result =
                    r.getBody(WorkflowService.StartWorkflowExecution_result.class);
                if (r.getResponseCode() == ResponseCode.OK) {
                  resultHandler.onComplete(result.getSuccess());
                  return;
                }
                if (result.isSetBadRequestError()) {
                  resultHandler.onError(result.getBadRequestError());
                  return;
                }
                if (result.isSetSessionAlreadyExistError()) {
                  resultHandler.onError(result.getSessionAlreadyExistError());
                  return;
                }
                if (result.isSetServiceBusyError()) {
                  resultHandler.onError(result.getServiceBusyError());
                  return;
                }
                if (result.isSetDomainNotActiveError()) {
                  resultHandler.onError(result.getDomainNotActiveError());
                  return;
                }
                if (result.isSetLimitExceededError()) {
                  resultHandler.onError(result.getLimitExceededError());
                  return;
                }
                if (result.isSetEntityNotExistError()) {
                  resultHandler.onError(result.getEntityNotExistError());
                  return;
                }
                if (result.isSetClientVersionNotSupportedError()) {
                  resultHandler.onError(result.getClientVersionNotSupportedError());
                }
                resultHandler.onError(
                    new TException("StartWorkflowExecution failed with unknown error:" + result));
              } finally {
                if (r != null) {
                  r.release();
                }
              }
            })
        .exceptionally(
            (e) -> {
              log.error("Unexpected error in StartWorkflowExecution", e);
              return null;
            });
  }

  @Override
  public void StartWorkflowExecutionAsync(
      StartWorkflowExecutionAsyncRequest startRequest, AsyncMethodCallback resultHandler)
      throws TException {
    startWorkflowExecutionAsync(startRequest, resultHandler, null);
  }

  @Override
  public void StartWorkflowExecutionAsyncWithTimeout(
      StartWorkflowExecutionAsyncRequest startAsyncRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException {
    startWorkflowExecutionAsync(startAsyncRequest, resultHandler, timeoutInMillis);
  }

  private void startWorkflowExecutionAsync(
      StartWorkflowExecutionAsyncRequest startAsyncRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis)
      throws TException {
    initializeStartWorkflowRequest(startAsyncRequest.getRequest());
    timeoutInMillis = validateAndUpdateTimeout(timeoutInMillis, options.getRpcTimeoutMillis());
    ThriftRequest<WorkflowService.StartWorkflowExecutionAsync_args> request =
        buildThriftRequest(
            "StartWorkflowExecutionAsync",
            new WorkflowService.StartWorkflowExecutionAsync_args(startAsyncRequest),
            timeoutInMillis);

    CompletableFuture<ThriftResponse<WorkflowService.StartWorkflowExecutionAsync_result>> response =
        doRemoteCallAsync(request);
    response
        .whenComplete(
            (r, e) -> {
              try {
                if (e != null) {
                  resultHandler.onError(CheckedExceptionWrapper.wrap(e));
                  return;
                }
                WorkflowService.StartWorkflowExecutionAsync_result result =
                    r.getBody(WorkflowService.StartWorkflowExecutionAsync_result.class);
                if (r.getResponseCode() == ResponseCode.OK) {
                  resultHandler.onComplete(result.getSuccess());
                  return;
                }
                if (result.isSetBadRequestError()) {
                  resultHandler.onError(result.getBadRequestError());
                  return;
                }
                if (result.isSetSessionAlreadyExistError()) {
                  resultHandler.onError(result.getSessionAlreadyExistError());
                  return;
                }
                if (result.isSetServiceBusyError()) {
                  resultHandler.onError(result.getServiceBusyError());
                  return;
                }
                if (result.isSetDomainNotActiveError()) {
                  resultHandler.onError(result.getDomainNotActiveError());
                  return;
                }
                if (result.isSetLimitExceededError()) {
                  resultHandler.onError(result.getLimitExceededError());
                  return;
                }
                if (result.isSetEntityNotExistError()) {
                  resultHandler.onError(result.getEntityNotExistError());
                  return;
                }
                if (result.isSetClientVersionNotSupportedError()) {
                  resultHandler.onError(result.getClientVersionNotSupportedError());
                }
                resultHandler.onError(
                    new TException(
                        "StartWorkflowExecutionAsync failed with unknown error:" + result));
              } finally {
                if (r != null) {
                  r.release();
                }
              }
            })
        .exceptionally(
            (e) -> {
              log.error("Unexpected error in StartWorkflowExecutionAsync", e);
              return null;
            });
  }

  private Long validateAndUpdateTimeout(Long timeoutInMillis, Long defaultTimeoutInMillis) {
    if (timeoutInMillis == null || timeoutInMillis <= 0 || timeoutInMillis == Long.MAX_VALUE) {
      timeoutInMillis = defaultTimeoutInMillis;
    } else {
      timeoutInMillis = Math.min(timeoutInMillis, defaultTimeoutInMillis);
    }
    return timeoutInMillis;
  }

  @SuppressWarnings({"unchecked", "FutureReturnValueIgnored"})
  @Override
  public void GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis) {

    getWorkflowExecutionHistory(getRequest, resultHandler, timeoutInMillis);
  }

  @SuppressWarnings({"unchecked", "FutureReturnValueIgnored"})
  @Override
  public void GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest, AsyncMethodCallback resultHandler) {

    getWorkflowExecutionHistory(getRequest, resultHandler, null);
  }

  private void getWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis) {

    ThriftRequest<WorkflowService.GetWorkflowExecutionHistory_args> request =
        buildGetWorkflowExecutionHistoryThriftRequest(getRequest, timeoutInMillis);

    CompletableFuture<ThriftResponse<GetWorkflowExecutionHistory_result>> response =
        doRemoteCallAsync(request);
    response
        .whenComplete(
            (r, e) -> {
              try {
                if (e != null) {
                  resultHandler.onError(CheckedExceptionWrapper.wrap(e));
                  return;
                }
                WorkflowService.GetWorkflowExecutionHistory_result result =
                    r.getBody(WorkflowService.GetWorkflowExecutionHistory_result.class);

                if (r.getResponseCode() == ResponseCode.OK) {
                  GetWorkflowExecutionHistoryResponse res = result.getSuccess();
                  if (res.getRawHistory() != null) {
                    History history =
                        InternalUtils.DeserializeFromBlobDataToHistory(
                            res.getRawHistory(), getRequest.getHistoryEventFilterType());
                    res.setHistory(history);
                  }
                  resultHandler.onComplete(res);
                  return;
                }
                if (result.isSetBadRequestError()) {
                  resultHandler.onError(result.getBadRequestError());
                  return;
                }
                if (result.isSetEntityNotExistError()) {
                  resultHandler.onError(result.getEntityNotExistError());
                  return;
                }
                if (result.isSetServiceBusyError()) {
                  resultHandler.onError(result.getServiceBusyError());
                  return;
                }
                resultHandler.onError(
                    new TException(
                        "GetWorkflowExecutionHistory failed with unknown " + "error:" + result));
              } catch (TException tException) {
                resultHandler.onError(tException);
              } finally {
                if (r != null) {
                  r.release();
                }
              }
            })
        .exceptionally(
            (e) -> {
              log.error("Unexpected error in GetWorkflowExecutionHistory", e);
              return null;
            });
  }

  @Override
  public void PollForDecisionTask(
      PollForDecisionTaskRequest pollRequest, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondDecisionTaskFailed(
      RespondDecisionTaskFailedRequest failedRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void PollForActivityTask(
      PollForActivityTaskRequest pollRequest, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskCompleted(
      RespondActivityTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskFailed(
      RespondActivityTaskFailedRequest failRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskFailedByID(
      RespondActivityTaskFailedByIDRequest failRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskCanceled(
      RespondActivityTaskCanceledRequest canceledRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RequestCancelWorkflowExecution(
      RequestCancelWorkflowExecutionRequest cancelRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void SignalWorkflowExecution(
      SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback resultHandler) {
    signalWorkflowExecution(signalRequest, resultHandler, null);
  }

  @Override
  public void SignalWorkflowExecutionWithTimeout(
      SignalWorkflowExecutionRequest signalRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis) {
    signalWorkflowExecution(signalRequest, resultHandler, timeoutInMillis);
  }

  private void signalWorkflowExecution(
      SignalWorkflowExecutionRequest signalRequest,
      AsyncMethodCallback resultHandler,
      Long timeoutInMillis) {

    timeoutInMillis = validateAndUpdateTimeout(timeoutInMillis, options.getRpcTimeoutMillis());
    ThriftRequest<WorkflowService.SignalWorkflowExecution_args> request =
        buildThriftRequest(
            "SignalWorkflowExecution",
            new WorkflowService.SignalWorkflowExecution_args(signalRequest),
            timeoutInMillis);
    CompletableFuture<ThriftResponse<WorkflowService.SignalWorkflowExecution_result>> response =
        doRemoteCallAsync(request);
    response
        .whenComplete(
            (r, e) -> {
              try {
                if (e != null) {
                  resultHandler.onError(CheckedExceptionWrapper.wrap(e));
                  return;
                }
                WorkflowService.SignalWorkflowExecution_result result =
                    r.getBody(WorkflowService.SignalWorkflowExecution_result.class);
                if (r.getResponseCode() == ResponseCode.OK) {
                  resultHandler.onComplete(null);
                  return;
                }
                if (result.isSetBadRequestError()) {
                  resultHandler.onError(result.getBadRequestError());
                  return;
                }
                if (result.isSetEntityNotExistError()) {
                  resultHandler.onError(result.getEntityNotExistError());
                  return;
                }
                if (result.isSetWorkflowExecutionAlreadyCompletedError()) {
                  resultHandler.onError(result.getWorkflowExecutionAlreadyCompletedError());
                  return;
                }
                if (result.isSetServiceBusyError()) {
                  resultHandler.onError(result.getServiceBusyError());
                  return;
                }
                if (result.isSetDomainNotActiveError()) {
                  resultHandler.onError(result.getDomainNotActiveError());
                  return;
                }
                if (result.isSetLimitExceededError()) {
                  resultHandler.onError(result.getLimitExceededError());
                  return;
                }
                if (result.isSetClientVersionNotSupportedError()) {
                  resultHandler.onError(result.getClientVersionNotSupportedError());
                  return;
                }
                resultHandler.onError(
                    new TException("SignalWorkflowExecution failed with unknown error:" + result));
              } finally {
                if (r != null) {
                  r.release();
                }
              }
            })
        .exceptionally(
            (e) -> {
              log.error("Unexpected error in SignalWorkflowExecution", e);
              return null;
            });
  }

  @Override
  public void SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest,
      AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void SignalWithStartWorkflowExecutionAsync(
      SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest,
      AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("unimplemented");
  }

  @Override
  public void ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void TerminateWorkflowExecution(
      TerminateWorkflowExecutionRequest terminateRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void GetSearchAttributes(AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RespondQueryTaskCompleted(
      RespondQueryTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void ResetStickyTaskList(
      ResetStickyTaskListRequest resetRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void QueryWorkflow(QueryWorkflowRequest queryRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void DescribeTaskList(DescribeTaskListRequest request, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void GetClusterInfo(AsyncMethodCallback resultHandler) throws TException {}

  @Override
  public void ListTaskListPartitions(
      ListTaskListPartitionsRequest request, AsyncMethodCallback resultHandler) throws TException {}

  @Override
  public void RefreshWorkflowTasks(
      RefreshWorkflowTasksRequest request, AsyncMethodCallback resultHandler) throws TException {}

  @Override
  public void RegisterDomain(
      RegisterDomainRequest registerRequest, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void DescribeDomain(
      DescribeDomainRequest describeRequest, AsyncMethodCallback resultHandler) throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void DiagnoseWorkflowExecution(
      DiagnoseWorkflowExecutionRequest diagnoseRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("DiagnoseWorkflowExecution is not implemented");
  }

  @Override
  public void ListDomains(ListDomainsRequest listRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void UpdateDomain(UpdateDomainRequest updateRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void DeprecateDomain(
      DeprecateDomainRequest deprecateRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void RestartWorkflowExecution(
      RestartWorkflowExecutionRequest restartRequest, AsyncMethodCallback resultHandler)
      throws TException {
    throw new UnsupportedOperationException("unimplemented");
  }

  @Override
  public void GetTaskListsByDomain(
      GetTaskListsByDomainRequest request, AsyncMethodCallback resultHandler)
      throws org.apache.thrift.TException {
    throw new UnsupportedOperationException("not implemented");
  }
}
