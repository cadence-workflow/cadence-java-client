/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
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
package com.uber.cadence.internal.compatibility.proto.serviceclient;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.uber.cadence.api.v1.*;
import com.uber.cadence.api.v1.DomainAPIGrpc;
import com.uber.cadence.api.v1.MetaAPIGrpc;
import com.uber.cadence.api.v1.MetaAPIGrpc.MetaAPIBlockingStub;
import com.uber.cadence.api.v1.MetaAPIGrpc.MetaAPIFutureStub;
import com.uber.cadence.api.v1.VisibilityAPIGrpc;
import com.uber.cadence.api.v1.VisibilityAPIGrpc.VisibilityAPIBlockingStub;
import com.uber.cadence.api.v1.VisibilityAPIGrpc.VisibilityAPIFutureStub;
import com.uber.cadence.api.v1.WorkerAPIGrpc;
import com.uber.cadence.api.v1.WorkerAPIGrpc.WorkerAPIBlockingStub;
import com.uber.cadence.api.v1.WorkerAPIGrpc.WorkerAPIFutureStub;
import com.uber.cadence.api.v1.WorkflowAPIGrpc;
import com.uber.cadence.api.v1.WorkflowAPIGrpc.WorkflowAPIBlockingStub;
import com.uber.cadence.api.v1.WorkflowAPIGrpc.WorkflowAPIFutureStub;
import com.uber.cadence.internal.Version;
import com.uber.cadence.internal.tracing.TracingPropagator;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.auth.IAuthorizationProvider;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrpcServiceStubs implements IGrpcServiceStubs {

  private static final Logger log = LoggerFactory.getLogger(GrpcServiceStubs.class);
  private static final Metadata.Key<String> LIBRARY_VERSION_HEADER_KEY =
      Metadata.Key.of("cadence-client-library-version", Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<String> FEATURE_VERSION_HEADER_KEY =
      Metadata.Key.of("cadence-client-feature-version", Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<String> CLIENT_IMPL_HEADER_KEY =
      Metadata.Key.of("cadence-client-name", Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<String> CALLER_TYPE_HEADER_KEY =
      Metadata.Key.of("cadence-caller-type", Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<String> ISOLATION_GROUP_HEADER_KEY =
      Metadata.Key.of("cadence-client-isolation-group", Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<String> RPC_SERVICE_NAME_HEADER_KEY =
      Metadata.Key.of("rpc-service", Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<String> RPC_CALLER_NAME_HEADER_KEY =
      Metadata.Key.of("rpc-caller", Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<String> RPC_ENCODING_HEADER_KEY =
      Metadata.Key.of("rpc-encoding", Metadata.ASCII_STRING_MARSHALLER);

  private static final Metadata.Key<String> AUTHORIZATION_HEADER_KEY =
      Metadata.Key.of("cadence-authorization", Metadata.ASCII_STRING_MARSHALLER);

  private static final String CLIENT_IMPL_HEADER_VALUE = "uber-java";
  private static final String CALLER_TYPE_HEADER_VALUE = "sdk";

  private final ClientOptions options;
  private final ManagedChannel channel;
  private final boolean shutdownChannel;
  private final AtomicBoolean shutdownRequested = new AtomicBoolean();
  private final DomainAPIGrpc.DomainAPIBlockingStub domainBlockingStub;
  private final DomainAPIGrpc.DomainAPIFutureStub domainFutureStub;
  private final VisibilityAPIGrpc.VisibilityAPIBlockingStub visibilityBlockingStub;
  private final VisibilityAPIGrpc.VisibilityAPIFutureStub visibilityFutureStub;
  private final WorkerAPIGrpc.WorkerAPIBlockingStub workerBlockingStub;
  private final WorkerAPIGrpc.WorkerAPIFutureStub workerFutureStub;
  private final WorkflowAPIGrpc.WorkflowAPIBlockingStub workflowBlockingStub;
  private final WorkflowAPIGrpc.WorkflowAPIFutureStub workflowFutureStub;
  private final MetaAPIGrpc.MetaAPIBlockingStub metaBlockingStub;
  private final MetaAPIGrpc.MetaAPIFutureStub metaFutureStub;

  GrpcServiceStubs(ClientOptions options) {
    this.options = options;
    if (options.getGRPCChannel() != null) {
      this.channel = options.getGRPCChannel();
      shutdownChannel = false;
    } else {
      this.channel =
          ManagedChannelBuilder.forAddress(options.getHost(), options.getPort())
              .defaultLoadBalancingPolicy("round_robin")
              .usePlaintext()
              .build();
      shutdownChannel = true;
    }
    ClientInterceptor deadlineInterceptor = new GrpcDeadlineInterceptor(options);
    ClientInterceptor tracingInterceptor = newTracingInterceptor();
    Metadata headers = new Metadata();
    headers.put(LIBRARY_VERSION_HEADER_KEY, Version.LIBRARY_VERSION);
    headers.put(FEATURE_VERSION_HEADER_KEY, Version.FEATURE_VERSION);
    headers.put(CLIENT_IMPL_HEADER_KEY, CLIENT_IMPL_HEADER_VALUE);
    headers.put(CALLER_TYPE_HEADER_KEY, CALLER_TYPE_HEADER_VALUE);
    headers.put(RPC_SERVICE_NAME_HEADER_KEY, options.getServiceName());
    headers.put(RPC_CALLER_NAME_HEADER_KEY, options.getClientAppName());
    headers.put(RPC_ENCODING_HEADER_KEY, "proto");
    if (!Strings.isNullOrEmpty(options.getIsolationGroup())) {
      headers.put(ISOLATION_GROUP_HEADER_KEY, options.getIsolationGroup());
    }

    Channel interceptedChannel =
        ClientInterceptors.intercept(
            channel,
            deadlineInterceptor,
            MetadataUtils.newAttachHeadersInterceptor(headers),
            newOpenTelemetryInterceptor(),
            newOpenTracingInterceptor(options.getTracer()));
    if (log.isTraceEnabled()) {
      interceptedChannel = ClientInterceptors.intercept(interceptedChannel, tracingInterceptor);
    }
    if (options.getAuthProvider() != null) {
      interceptedChannel =
          ClientInterceptors.intercept(
              interceptedChannel, newAuthorizationInterceptor(options.getAuthProvider()));
    }
    this.domainBlockingStub = DomainAPIGrpc.newBlockingStub(interceptedChannel);
    this.domainFutureStub = DomainAPIGrpc.newFutureStub(interceptedChannel);
    this.visibilityBlockingStub = VisibilityAPIGrpc.newBlockingStub(interceptedChannel);
    this.visibilityFutureStub = VisibilityAPIGrpc.newFutureStub(interceptedChannel);
    this.workerBlockingStub = WorkerAPIGrpc.newBlockingStub(interceptedChannel);
    this.workerFutureStub = WorkerAPIGrpc.newFutureStub(interceptedChannel);
    this.workflowBlockingStub = WorkflowAPIGrpc.newBlockingStub(interceptedChannel);
    this.workflowFutureStub = WorkflowAPIGrpc.newFutureStub(interceptedChannel);
    this.metaBlockingStub = MetaAPIGrpc.newBlockingStub(interceptedChannel);
    this.metaFutureStub = MetaAPIGrpc.newFutureStub(interceptedChannel);
  }

  private ClientInterceptor newAuthorizationInterceptor(IAuthorizationProvider provider) {
    return new ClientInterceptor() {
      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
          MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)) {

          @Override
          public void start(Listener<RespT> responseListener, Metadata headers) {
            headers.put(
                AUTHORIZATION_HEADER_KEY,
                new String(provider.getAuthToken(), StandardCharsets.UTF_8));

            Listener<RespT> listener =
                new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                    responseListener) {

                  @Override
                  public void onHeaders(Metadata headers) {
                    super.onHeaders(headers);
                  }
                };
            super.start(listener, headers);
          }
        };
      }
    };
  }

  private ClientInterceptor newOpenTelemetryInterceptor() {
    return new ClientInterceptor() {
      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
          MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)) {

          @Override
          public void start(Listener<RespT> responseListener, Metadata headers) {
            TextMapPropagator propagator =
                GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

            final TextMapSetter<Metadata> setter =
                (carrier, key, value) -> {
                  if (carrier != null) {
                    carrier.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);
                  }
                };
            if (propagator != null) {
              propagator.inject(Context.current(), headers, setter);
            }

            super.start(responseListener, headers);
          }
        };
      }
    };
  }

  private ClientInterceptor newOpenTracingInterceptor(Tracer tracer) {
    return new ClientInterceptor() {
      private final TracingPropagator tracingPropagator = new TracingPropagator(tracer);
      private final String OPERATIONFORMAT = "cadence-%s";

      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
          MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)) {

          @Override
          public void start(Listener<RespT> responseListener, Metadata headers) {
            Span span =
                tracingPropagator.spanByServiceMethod(
                    String.format(OPERATIONFORMAT, method.getBareMethodName()));
            Scope scope = tracer.activateSpan(span);
            super.start(
                new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                    responseListener) {
                  @Override
                  public void onClose(Status status, Metadata trailers) {
                    try {
                      super.onClose(status, trailers);
                    } finally {
                      span.finish();
                      scope.close();
                    }
                  }
                },
                headers);
          }

          @SuppressWarnings("unchecked")
          @Override
          public void sendMessage(ReqT message) {
            if (Objects.equals(method.getBareMethodName(), "StartWorkflowExecution")
                && message instanceof StartWorkflowExecutionRequest) {
              StartWorkflowExecutionRequest request = (StartWorkflowExecutionRequest) message;
              Header newHeader = addTracingHeaders(request.getHeader());

              // cast should not throw error as we are using the builder
              message = (ReqT) request.toBuilder().setHeader(newHeader).build();
            } else if (Objects.equals(method.getBareMethodName(), "StartWorkflowExecutionAsync")
                && message instanceof StartWorkflowExecutionAsyncRequest) {
              StartWorkflowExecutionAsyncRequest request =
                  (StartWorkflowExecutionAsyncRequest) message;
              Header newHeader = addTracingHeaders(request.getRequest().getHeader());

              // cast should not throw error as we are using the builder
              message =
                  (ReqT)
                      request
                          .toBuilder()
                          .setRequest(request.getRequest().toBuilder().setHeader(newHeader))
                          .build();
            } else if (Objects.equals(
                    method.getBareMethodName(), "SignalWithStartWorkflowExecution")
                && message instanceof SignalWithStartWorkflowExecutionRequest) {
              SignalWithStartWorkflowExecutionRequest request =
                  (SignalWithStartWorkflowExecutionRequest) message;
              Header newHeader = addTracingHeaders(request.getStartRequest().getHeader());

              // cast should not throw error as we are using the builder
              message =
                  (ReqT)
                      request
                          .toBuilder()
                          .setStartRequest(
                              request.getStartRequest().toBuilder().setHeader(newHeader))
                          .build();
            } else if (Objects.equals(
                    method.getBareMethodName(), "SignalWithStartWorkflowExecutionAsync")
                && message instanceof SignalWithStartWorkflowExecutionAsyncRequest) {
              SignalWithStartWorkflowExecutionAsyncRequest request =
                  (SignalWithStartWorkflowExecutionAsyncRequest) message;
              Header newHeader =
                  addTracingHeaders(request.getRequest().getStartRequest().getHeader());

              // cast should not throw error as we are using the builder
              message =
                  (ReqT)
                      request
                          .toBuilder()
                          .setRequest(
                              request
                                  .getRequest()
                                  .toBuilder()
                                  .setStartRequest(
                                      request
                                          .getRequest()
                                          .getStartRequest()
                                          .toBuilder()
                                          .setHeader(newHeader)))
                          .build();
            }
            super.sendMessage(message);
          }

          private Header addTracingHeaders(Header header) {
            Map<String, byte[]> headers = new HashMap<>();
            tracingPropagator.inject(headers);
            Header.Builder headerBuilder = header.toBuilder();
            headers.forEach(
                (k, v) ->
                    headerBuilder.putFields(
                        k, Payload.newBuilder().setData(ByteString.copyFrom(v)).build()));
            return headerBuilder.build();
          }
        };
      }
    };
  }

  private ClientInterceptor newTracingInterceptor() {
    return new ClientInterceptor() {

      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
          MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)) {
          @Override
          public void sendMessage(ReqT message) {
            log.trace("Invoking " + method.getFullMethodName() + "with input: " + message);
            super.sendMessage(message);
          }

          @Override
          public void start(Listener<RespT> responseListener, Metadata headers) {
            Listener<RespT> listener =
                new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                    responseListener) {
                  @Override
                  public void onMessage(RespT message) {
                    if (method == WorkerAPIGrpc.getPollForDecisionTaskMethod()) {
                      log.trace("Returned " + method.getFullMethodName());
                    } else {
                      log.trace(
                          "Returned " + method.getFullMethodName() + " with output: " + message);
                    }
                    super.onMessage(message);
                  }
                };
            super.start(listener, headers);
          }
        };
      }
    };
  }

  public DomainAPIGrpc.DomainAPIBlockingStub domainBlockingStub() {
    return domainBlockingStub;
  }

  public DomainAPIGrpc.DomainAPIFutureStub domainFutureStub() {
    return domainFutureStub;
  }

  @Override
  public ClientOptions getOptions() {
    return options;
  }

  @Override
  public VisibilityAPIBlockingStub visibilityBlockingStub() {
    return visibilityBlockingStub;
  }

  @Override
  public VisibilityAPIFutureStub visibilityFutureStub() {
    return visibilityFutureStub;
  }

  @Override
  public WorkerAPIBlockingStub workerBlockingStub() {
    return workerBlockingStub;
  }

  @Override
  public WorkerAPIFutureStub workerFutureStub() {
    return workerFutureStub;
  }

  @Override
  public WorkflowAPIBlockingStub workflowBlockingStub() {
    return workflowBlockingStub;
  }

  @Override
  public MetaAPIFutureStub metaFutureStub() {
    return metaFutureStub;
  }

  @Override
  public MetaAPIBlockingStub metaBlockingStub() {
    return metaBlockingStub;
  }

  @Override
  public WorkflowAPIFutureStub workflowFutureStub() {
    return workflowFutureStub;
  }

  @Override
  public void shutdown() {
    shutdownRequested.set(true);
    if (shutdownChannel) {
      channel.shutdown();
    }
  }

  @Override
  public void shutdownNow() {
    shutdownRequested.set(true);
    if (shutdownChannel) {
      channel.shutdownNow();
    }
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    if (shutdownChannel) {
      return channel.awaitTermination(timeout, unit);
    }
    return true;
  }

  @Override
  public boolean isShutdown() {
    if (shutdownChannel) {
      return channel.isShutdown();
    }
    return shutdownRequested.get();
  }

  @Override
  public boolean isTerminated() {
    if (shutdownChannel) {
      return channel.isTerminated();
    }
    return shutdownRequested.get();
  }

  private static class GrpcDeadlineInterceptor implements ClientInterceptor {

    private final ClientOptions options;

    public GrpcDeadlineInterceptor(ClientOptions options) {
      this.options = options;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
      Deadline deadline = callOptions.getDeadline();
      long duration;
      if (deadline == null) {
        duration = options.getRpcTimeoutMillis();
      } else {
        duration = deadline.timeRemaining(TimeUnit.MILLISECONDS);
      }
      if (method == WorkflowAPIGrpc.getGetWorkflowExecutionHistoryMethod()) {
        if (deadline == null) {
          duration = options.getRpcLongPollTimeoutMillis();
        } else {
          duration = deadline.timeRemaining(TimeUnit.MILLISECONDS);
          if (duration > options.getRpcLongPollTimeoutMillis()) {
            duration = options.getRpcLongPollTimeoutMillis();
          }
        }
      } else if (method == WorkerAPIGrpc.getPollForDecisionTaskMethod()
          || method == WorkerAPIGrpc.getPollForActivityTaskMethod()) {
        duration = options.getRpcLongPollTimeoutMillis();
      } else if (method == WorkflowAPIGrpc.getQueryWorkflowMethod()) {
        duration = options.getRpcQueryTimeoutMillis();
      }
      if (log.isTraceEnabled()) {
        String name = method.getFullMethodName();
        log.trace("TimeoutInterceptor method=" + name + ", timeoutMs=" + duration);
      }
      return next.newCall(method, callOptions.withDeadlineAfter(duration, TimeUnit.MILLISECONDS));
    }
  }
}
