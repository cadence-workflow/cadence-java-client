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

package com.uber.cadence.internal.worker;

import com.uber.cadence.context.ContextPropagator;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.internal.metrics.NoopScope;
import com.uber.cadence.worker.ExecutorWrapper;
import com.uber.m3.tally.Scope;
import io.opentracing.Tracer;
import java.time.Duration;
import java.util.List;

public final class SingleWorkerOptions {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(SingleWorkerOptions options) {
    return new Builder(options);
  }

  public static final class Builder {
    private String identity;
    private DataConverter dataConverter;
    private int taskExecutorThreadPoolSize = 100;
    private double taskListActivitiesPerSecond;
    private PollerOptions pollerOptions;
    private Scope metricsScope;
    private boolean enableLoggingInReplay;
    private List<ContextPropagator> contextPropagators;
    private Tracer tracer;
    private ExecutorWrapper executorWrapper;
    private Duration stickyTaskListScheduleToStartTimeout;

    private Builder() {}

    public Builder(SingleWorkerOptions options) {
      this.identity = options.getIdentity();
      this.dataConverter = options.getDataConverter();
      this.pollerOptions = options.getPollerOptions();
      this.taskListActivitiesPerSecond = options.getTaskListActivitiesPerSecond();
      this.taskExecutorThreadPoolSize = options.getTaskExecutorThreadPoolSize();
      this.metricsScope = options.getMetricsScope();
      this.enableLoggingInReplay = options.getEnableLoggingInReplay();
      this.contextPropagators = options.getContextPropagators();
      this.tracer = options.getTracer();
      this.executorWrapper = options.getExecutorWrapper();
      this.stickyTaskListScheduleToStartTimeout = options.getStickyTaskListScheduleToStartTimeout();
    }

    public Builder setIdentity(String identity) {
      this.identity = identity;
      return this;
    }

    public Builder setDataConverter(DataConverter dataConverter) {
      this.dataConverter = dataConverter;
      return this;
    }

    public Builder setTaskExecutorThreadPoolSize(int taskExecutorThreadPoolSize) {
      this.taskExecutorThreadPoolSize = taskExecutorThreadPoolSize;
      return this;
    }

    public Builder setPollerOptions(PollerOptions pollerOptions) {
      this.pollerOptions = pollerOptions;
      return this;
    }

    public Builder setMetricsScope(Scope metricsScope) {
      this.metricsScope = metricsScope;
      return this;
    }

    public Builder setEnableLoggingInReplay(boolean enableLoggingInReplay) {
      this.enableLoggingInReplay = enableLoggingInReplay;
      return this;
    }

    public Builder setTaskListActivitiesPerSecond(double taskListActivitiesPerSecond) {
      this.taskListActivitiesPerSecond = taskListActivitiesPerSecond;
      return this;
    }

    /** Specifies the list of context propagators to use during this workflow. */
    public Builder setContextPropagators(List<ContextPropagator> contextPropagators) {
      this.contextPropagators = contextPropagators;
      return this;
    }

    public Builder setTracer(Tracer tracer) {
      this.tracer = tracer;
      return this;
    }

    public Builder setExecutorWrapper(ExecutorWrapper executorWrapper) {
      this.executorWrapper = executorWrapper;
      return this;
    }

    public Builder setStickyTaskListScheduleToStartTimeout(
        Duration stickyTaskListScheduleToStartTimeout) {
      this.stickyTaskListScheduleToStartTimeout = stickyTaskListScheduleToStartTimeout;
      return this;
    }

    public SingleWorkerOptions build() {
      if (pollerOptions == null) {
        pollerOptions =
            PollerOptions.newBuilder()
                .setPollBackoffInitialInterval(Duration.ofMillis(200))
                .setPollBackoffMaximumInterval(Duration.ofSeconds(20))
                .setPollThreadCount(1)
                .build();
      }

      if (dataConverter == null) {
        dataConverter = JsonDataConverter.getInstance();
      }

      if (metricsScope == null) {
        metricsScope = NoopScope.getInstance();
      }

      return new SingleWorkerOptions(
          identity,
          dataConverter,
          taskExecutorThreadPoolSize,
          taskListActivitiesPerSecond,
          pollerOptions,
          metricsScope,
          enableLoggingInReplay,
          contextPropagators,
          tracer,
          executorWrapper,
          stickyTaskListScheduleToStartTimeout);
    }
  }

  private final String identity;
  private final DataConverter dataConverter;
  private final int taskExecutorThreadPoolSize;
  private final double taskListActivitiesPerSecond;
  private final PollerOptions pollerOptions;
  private final Scope metricsScope;
  private final boolean enableLoggingInReplay;
  private List<ContextPropagator> contextPropagators;
  private final Tracer tracer;
  private final ExecutorWrapper executorWrapper;
  private final Duration stickyTaskListScheduleToStartTimeout;

  private SingleWorkerOptions(
      String identity,
      DataConverter dataConverter,
      int taskExecutorThreadPoolSize,
      double taskListActivitiesPerSecond,
      PollerOptions pollerOptions,
      Scope metricsScope,
      boolean enableLoggingInReplay,
      List<ContextPropagator> contextPropagators,
      Tracer tracer,
      ExecutorWrapper executorWrapper,
      Duration stickyTaskListScheduleToStartTimeout) {
    this.identity = identity;
    this.dataConverter = dataConverter;
    this.taskExecutorThreadPoolSize = taskExecutorThreadPoolSize;
    this.taskListActivitiesPerSecond = taskListActivitiesPerSecond;
    this.pollerOptions = pollerOptions;
    this.metricsScope = metricsScope;
    this.enableLoggingInReplay = enableLoggingInReplay;
    this.contextPropagators = contextPropagators;
    this.tracer = tracer;
    this.executorWrapper = executorWrapper;
    this.stickyTaskListScheduleToStartTimeout = stickyTaskListScheduleToStartTimeout;
  }

  public String getIdentity() {
    return identity;
  }

  public DataConverter getDataConverter() {
    return dataConverter;
  }

  int getTaskExecutorThreadPoolSize() {
    return taskExecutorThreadPoolSize;
  }

  PollerOptions getPollerOptions() {
    return pollerOptions;
  }

  public double getTaskListActivitiesPerSecond() {
    return taskListActivitiesPerSecond;
  }

  public Scope getMetricsScope() {
    return metricsScope;
  }

  public boolean getEnableLoggingInReplay() {
    return enableLoggingInReplay;
  }

  public List<ContextPropagator> getContextPropagators() {
    return contextPropagators;
  }

  public Tracer getTracer() {
    return tracer;
  }

  public ExecutorWrapper getExecutorWrapper() {
    return executorWrapper;
  }

  public Duration getStickyTaskListScheduleToStartTimeout() {
    return stickyTaskListScheduleToStartTimeout;
  }
}
