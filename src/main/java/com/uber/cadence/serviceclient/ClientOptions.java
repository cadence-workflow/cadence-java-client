/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
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

package com.uber.cadence.serviceclient;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.uber.cadence.FeatureFlags;
import com.uber.cadence.internal.metrics.NoopScope;
import com.uber.cadence.serviceclient.auth.IAuthorizationProvider;
import com.uber.m3.tally.Scope;
import io.grpc.ManagedChannel;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;
import java.util.Map;

public class ClientOptions {

  private static final int DEFAULT_LOCAL_CADENCE_SERVER_PORT = 7933;

  private static final String LOCALHOST = "127.0.0.1";

  /** Default RPC timeout used for all non long poll calls. */
  private static final long DEFAULT_RPC_TIMEOUT_MILLIS = 3 * 1000;
  /** Default RPC timeout used for all long poll calls. */
  private static final long DEFAULT_POLL_RPC_TIMEOUT_MILLIS = 30 * 1000;

  /** Default RPC timeout for QueryWorkflow */
  private static final long DEFAULT_QUERY_RPC_TIMEOUT_MILLIS = 10 * 1000;

  /** Default RPC timeout for ListArchivedWorkflow */
  private static final long DEFAULT_LIST_ARCHIVED_WORKFLOW_TIMEOUT_MILLIS = 180 * 1000;

  private static final String DEFAULT_CLIENT_APP_NAME = "cadence-client";

  /** Name of the Cadence service front end as required by TChannel. */
  private static final String DEFAULT_SERVICE_NAME = "cadence-frontend";

  private static final ClientOptions DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = new Builder().build();
  }

  private final String host;
  private final int port;
  private final ManagedChannel gRPCChannel;
  /** The tChannel timeout in milliseconds */
  private final long rpcTimeoutMillis;
  /** The tChannel timeout for long poll calls in milliseconds */
  private final long rpcLongPollTimeoutMillis;
  /** The tChannel timeout for query workflow call in milliseconds */
  private final long rpcQueryTimeoutMillis;
  /** The tChannel timeout for list archived workflow call in milliseconds */
  private final long rpcListArchivedWorkflowTimeoutMillis;
  /** TChannel service name that the Cadence service was started with. */
  private final String serviceName;
  /** Name of the service using the cadence-client. */
  private final String clientAppName;
  /** Client for metrics reporting. */
  private final Scope metricsScope;
  /** Optional TChannel transport headers */
  private final Map<String, String> transportHeaders;
  /** Optional TChannel headers */
  private final Map<String, String> headers;
  /** Optional authorization provider */
  private final IAuthorizationProvider authProvider;
  /** Optional Feature flags to turn on/off some Cadence features */
  private final FeatureFlags featureFlags;
  /** Optional isolation group of the service if tasklist isolation is enabled */
  private final String isolationGroup;
  /** Optional tracer for service client, default is GlobalTracer */
  private final Tracer tracer;

  private ClientOptions(Builder builder) {
    if (Strings.isNullOrEmpty(builder.host)) {
      host =
          Strings.isNullOrEmpty(System.getenv("CADENCE_SEEDS"))
              ? LOCALHOST
              : System.getenv("CADENCE_SEEDS");
    } else {
      host = builder.host;
    }
    this.port = builder.port;
    this.gRPCChannel = builder.gRPCChannel;
    this.rpcTimeoutMillis = builder.rpcTimeoutMillis;
    if (builder.clientAppName == null) {
      this.clientAppName = DEFAULT_CLIENT_APP_NAME;
    } else {
      this.clientAppName = builder.clientAppName;
    }
    if (builder.serviceName == null) {
      this.serviceName = DEFAULT_SERVICE_NAME;
    } else {
      this.serviceName = builder.serviceName;
    }
    this.rpcLongPollTimeoutMillis = builder.rpcLongPollTimeoutMillis;
    this.rpcQueryTimeoutMillis = builder.rpcQueryTimeoutMillis;
    this.rpcListArchivedWorkflowTimeoutMillis = builder.rpcListArchivedWorkflowTimeoutMillis;
    if (builder.metricsScope == null) {
      builder.metricsScope = NoopScope.getInstance();
    }
    this.metricsScope = builder.metricsScope;
    if (builder.transportHeaders != null) {
      this.transportHeaders = ImmutableMap.copyOf(builder.transportHeaders);
    } else {
      this.transportHeaders = ImmutableMap.of();
    }

    this.featureFlags = builder.featureFlags;

    if (builder.headers != null) {
      this.headers = ImmutableMap.copyOf(builder.headers);
    } else {
      this.headers = ImmutableMap.of();
    }
    this.authProvider = builder.authProvider;
    this.isolationGroup = builder.isolationGroup;
    if (builder.tracer != null) {
      this.tracer = builder.tracer;
    } else {
      // Default value is GlobalTracer. If the user overrides it with null, fall back to NoopTracer.
      // We need some tracer instance
      this.tracer = NoopTracerFactory.create();
    }
  }

  public static ClientOptions defaultInstance() {
    return DEFAULT_INSTANCE;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public ManagedChannel getGRPCChannel() {
    return gRPCChannel;
  }

  /** @return Returns the rpc timeout value in millis. */
  public long getRpcTimeoutMillis() {
    return rpcTimeoutMillis;
  }

  /** @return Returns the rpc timout for long poll requests in millis. */
  public long getRpcLongPollTimeoutMillis() {
    return rpcLongPollTimeoutMillis;
  }

  /** @return Returns the rpc timout for query workflow requests in millis. */
  public long getRpcQueryTimeoutMillis() {
    return rpcQueryTimeoutMillis;
  }

  /** @return Returns the rpc timout for list archived workflow requests in millis. */
  public long getRpcListArchivedWorkflowTimeoutMillis() {
    return rpcListArchivedWorkflowTimeoutMillis;
  }

  /** Returns the client application name. */
  public String getClientAppName() {
    return this.clientAppName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public Scope getMetricsScope() {
    return metricsScope;
  }

  public Map<String, String> getTransportHeaders() {
    return transportHeaders;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public IAuthorizationProvider getAuthProvider() {
    return authProvider;
  }

  public FeatureFlags getFeatureFlags() {
    return this.featureFlags;
  }

  public String getIsolationGroup() {
    return this.isolationGroup;
  }

  public Tracer getTracer() {
    return this.tracer;
  }

  /**
   * Builder is the builder for ClientOptions.
   *
   * @author venkat
   */
  public static class Builder {

    private String host;
    private int port = DEFAULT_LOCAL_CADENCE_SERVER_PORT;
    private ManagedChannel gRPCChannel;
    private String clientAppName = DEFAULT_CLIENT_APP_NAME;
    private long rpcTimeoutMillis = DEFAULT_RPC_TIMEOUT_MILLIS;
    private long rpcLongPollTimeoutMillis = DEFAULT_POLL_RPC_TIMEOUT_MILLIS;
    private long rpcQueryTimeoutMillis = DEFAULT_QUERY_RPC_TIMEOUT_MILLIS;
    private long rpcListArchivedWorkflowTimeoutMillis =
        DEFAULT_LIST_ARCHIVED_WORKFLOW_TIMEOUT_MILLIS;
    private String serviceName;
    private Scope metricsScope;
    private Map<String, String> transportHeaders;
    private Map<String, String> headers;
    private IAuthorizationProvider authProvider;
    private FeatureFlags featureFlags;
    private String isolationGroup;
    // by default GlobalTracer
    private Tracer tracer = GlobalTracer.get();

    private Builder() {}

    public Builder setHost(String host) {
      this.host = host;
      return this;
    }

    public Builder setAuthorizationProvider(IAuthorizationProvider provider) {
      this.authProvider = provider;
      return this;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    /** Sets gRPC channel to use. Exclusive with host and port. */
    public Builder setGRPCChannel(ManagedChannel gRPCChannel) {
      this.gRPCChannel = gRPCChannel;
      return this;
    }

    /**
     * Sets the rpc timeout value for non query and non long poll calls. Default is 1000.
     *
     * @param timeoutMillis timeout, in millis.
     */
    public Builder setRpcTimeout(long timeoutMillis) {
      this.rpcTimeoutMillis = timeoutMillis;
      return this;
    }

    /**
     * Sets the rpc timeout value for the following long poll based operations: PollForDecisionTask,
     * PollForActivityTask, GetWorkflowExecutionHistory. Should never be below 60000 as this is
     * server side timeout for the long poll. Default is 61000.
     *
     * @param timeoutMillis timeout, in millis.
     */
    public Builder setRpcLongPollTimeout(long timeoutMillis) {
      this.rpcLongPollTimeoutMillis = timeoutMillis;
      return this;
    }

    /**
     * Sets the rpc timeout value for query calls. Default is 10000.
     *
     * @param timeoutMillis timeout, in millis.
     */
    public Builder setQueryRpcTimeout(long timeoutMillis) {
      this.rpcQueryTimeoutMillis = timeoutMillis;
      return this;
    }

    /**
     * Sets the rpc timeout value for query calls. Default is 180000.
     *
     * @param timeoutMillis timeout, in millis.
     */
    public Builder setListArchivedWorkflowRpcTimeout(long timeoutMillis) {
      this.rpcListArchivedWorkflowTimeoutMillis = timeoutMillis;
      return this;
    }

    /** Returns the feature flags defined in ClientOptions */
    public FeatureFlags getFeatureFlags() {
      return this.featureFlags;
    }

    /**
     * Sets the feature flags to turn on/off some Cadence features By default, all features under
     * FeatureFlags are turned off.
     *
     * @param featureFlags FeatureFlags
     */
    public Builder setFeatureFlags(FeatureFlags featureFlags) {
      this.featureFlags = featureFlags;
      return this;
    }

    /**
     * Sets the client application name.
     *
     * <p>This name will be used as the tchannel client service name. It will also be reported as a
     * tag along with metrics emitted to m3.
     *
     * @param clientAppName String representing the client application name.
     * @return Builder for ClentOptions
     */
    public Builder setClientAppName(String clientAppName) {
      this.clientAppName = clientAppName;
      return this;
    }

    /**
     * Sets the service name that Cadence service was started with.
     *
     * @param serviceName String representing the service name
     * @return Builder for ClentOptions
     */
    public Builder setServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    /**
     * Sets the metrics scope to be used for metrics reporting.
     *
     * @param metricsScope
     * @return Builder for ClentOptions
     */
    public Builder setMetricsScope(Scope metricsScope) {
      this.metricsScope = metricsScope;
      return this;
    }

    /**
     * Sets additional transport headers for tchannel client.
     *
     * @param transportHeaders Map with additional transport headers
     * @return Builder for ClentOptions
     */
    public Builder setTransportHeaders(Map<String, String> transportHeaders) {
      this.transportHeaders = transportHeaders;
      return this;
    }

    public Builder setHeaders(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    /**
     * Sets the isolation group to be used for matching.
     *
     * @param isolationGroup
     * @return Builder for ClentOptions
     */
    public Builder setIsolationGroup(String isolationGroup) {
      this.isolationGroup = isolationGroup;
      return this;
    }

    /**
     * Sets the tracer to be used for service client.
     *
     * @param tracer
     * @return Builder for ClentOptions
     */
    public Builder setTracer(Tracer tracer) {
      this.tracer = tracer;
      return this;
    }

    /**
     * Builds and returns a ClientOptions object.
     *
     * @return ClientOptions object with the specified params.
     */
    public ClientOptions build() {
      return new ClientOptions(this);
    }
  }
}
