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

import com.google.protobuf.ByteString;
import com.uber.cadence.api.v1.Header;
import com.uber.cadence.api.v1.Payload;
import com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionAsyncRequest;
import com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.api.v1.StartWorkflowExecutionAsyncRequest;
import com.uber.cadence.api.v1.StartWorkflowExecutionRequest;
import com.uber.cadence.internal.tracing.TracingPropagator;
import io.grpc.Attributes;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

final class OpenTracingInterceptor implements ClientInterceptor {
  private static final String OPERATION_FORMAT = "cadence-%s";
  private final Tracer tracer;
  private final TracingPropagator tracingPropagator;

  OpenTracingInterceptor(Tracer tracer) {
    this.tracer = tracer;
    this.tracingPropagator = new TracingPropagator(tracer);
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

    Span span =
        tracingPropagator.spanByServiceMethod(
            String.format(OPERATION_FORMAT, method.getBareMethodName()));
    try (Scope ignored = tracer.activateSpan(span)) {
      return new OpenTracingClientCall<>(next, method, callOptions, span);
    }
  }

  private class OpenTracingClientCall<ReqT, RespT>
      extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

    private final AtomicBoolean finished = new AtomicBoolean();
    private final MethodDescriptor<ReqT, RespT> method;
    private final Span span;

    public OpenTracingClientCall(
        Channel next, MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Span span) {
      super(next.newCall(method, callOptions));
      this.method = method;
      this.span = span;
    }

    @Override
    public void start(Listener<RespT> responseListener, Metadata headers) {
      try (Scope ignored = tracer.activateSpan(span)) {
        super.start(
            new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                responseListener) {
              @Override
              public void onClose(Status status, Metadata trailers) {
                try {
                  super.onClose(status, trailers);
                } finally {
                  finishSpan();
                }
              }
            },
            headers);
      }
    }

    @Override
    public void request(int numMessages) {
      try (Scope ignored = tracer.activateSpan(span)) {
        super.request(numMessages);
      }
    }

    @Override
    public void setMessageCompression(boolean enabled) {
      try (Scope ignored = tracer.activateSpan(span)) {
        super.setMessageCompression(enabled);
      }
    }

    @Override
    public boolean isReady() {
      try (Scope ignored = tracer.activateSpan(span)) {
        return super.isReady();
      }
    }

    @Override
    public Attributes getAttributes() {
      try (Scope ignored = tracer.activateSpan(span)) {
        return super.getAttributes();
      }
    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {
      try (Scope ignored = tracer.activateSpan(span)) {
        super.cancel(message, cause);
      } finally {
        finishSpan();
      }
    }

    @Override
    public void halfClose() {
      try (Scope ignored = tracer.activateSpan(span)) {
        super.halfClose();
      }
    }

    @Override
    public void sendMessage(ReqT message) {
      try (Scope ignored = tracer.activateSpan(span)) {
        message = replaceMessage(message);
        super.sendMessage(message);
      }
    }

    private void finishSpan() {
      // Some combination of cancel and onClose can be called so ensure we only finish once
      if (finished.compareAndSet(false, true)) {
        span.finish();
      }
    }

    @SuppressWarnings("unchecked")
    private ReqT replaceMessage(ReqT message) {
      if (Objects.equals(method.getBareMethodName(), "StartWorkflowExecution")
          && message instanceof StartWorkflowExecutionRequest) {
        StartWorkflowExecutionRequest request = (StartWorkflowExecutionRequest) message;
        Header newHeader = addTracingHeaders(request.getHeader());

        // cast should not throw error as we are using the builder
        message = (ReqT) request.toBuilder().setHeader(newHeader).build();
      } else if (Objects.equals(method.getBareMethodName(), "StartWorkflowExecutionAsync")
          && message instanceof StartWorkflowExecutionAsyncRequest) {
        StartWorkflowExecutionAsyncRequest request = (StartWorkflowExecutionAsyncRequest) message;
        Header newHeader = addTracingHeaders(request.getRequest().getHeader());

        // cast should not throw error as we are using the builder
        message =
            (ReqT)
                request
                    .toBuilder()
                    .setRequest(request.getRequest().toBuilder().setHeader(newHeader))
                    .build();
      } else if (Objects.equals(method.getBareMethodName(), "SignalWithStartWorkflowExecution")
          && message instanceof SignalWithStartWorkflowExecutionRequest) {
        SignalWithStartWorkflowExecutionRequest request =
            (SignalWithStartWorkflowExecutionRequest) message;
        Header newHeader = addTracingHeaders(request.getStartRequest().getHeader());

        // cast should not throw error as we are using the builder
        message =
            (ReqT)
                request
                    .toBuilder()
                    .setStartRequest(request.getStartRequest().toBuilder().setHeader(newHeader))
                    .build();
      } else if (Objects.equals(method.getBareMethodName(), "SignalWithStartWorkflowExecutionAsync")
          && message instanceof SignalWithStartWorkflowExecutionAsyncRequest) {
        SignalWithStartWorkflowExecutionAsyncRequest request =
            (SignalWithStartWorkflowExecutionAsyncRequest) message;
        Header newHeader = addTracingHeaders(request.getRequest().getStartRequest().getHeader());

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

      return message;
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
