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

package com.uber.cadence.testing;

import com.google.common.annotations.VisibleForTesting;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.worker.WorkerFactoryOptions;
import com.uber.cadence.workflow.WorkflowInterceptor;
import java.util.Objects;
import java.util.function.Function;

@VisibleForTesting
public final class TestEnvironmentOptions {

  public static final class Builder {

    private DataConverter dataConverter = JsonDataConverter.getInstance();

    private Function<WorkflowInterceptor, WorkflowInterceptor> interceptorFactory = (n) -> n;

    private boolean enableLoggingInReplay;

    private WorkerFactoryOptions factoryOptions;

    private WorkflowClientOptions workflowClientOptions = WorkflowClientOptions.defaultInstance();

    public Builder setWorkflowClientOptions(WorkflowClientOptions workflowClientOptions) {
      this.workflowClientOptions = workflowClientOptions;
      return this;
    }

    /** Sets data converter to use for unit-tests. Default is {@link JsonDataConverter}. */
    public Builder setDataConverter(DataConverter dataConverter) {
      this.dataConverter = Objects.requireNonNull(dataConverter);
      return this;
    }

    /**
     * Specifies an interceptor factory that creates interceptors for workflow calls like activity
     * invocations. Note that the factory is called for each decision and must return a new object
     * instance every time it is called.
     */
    public Builder setInterceptorFactory(
        Function<WorkflowInterceptor, WorkflowInterceptor> interceptorFactory) {
      this.interceptorFactory = Objects.requireNonNull(interceptorFactory);
      return this;
    }

    /** Set factoryOptions for worker factory used to create workers. */
    public Builder setWorkerFactoryOptions(WorkerFactoryOptions options) {
      this.factoryOptions = options;
      return this;
    }

    /** Set whether to log during decision replay. */
    public Builder setEnableLoggingInReplay(boolean enableLoggingInReplay) {
      this.enableLoggingInReplay = enableLoggingInReplay;
      return this;
    }

    public TestEnvironmentOptions build() {
      if (factoryOptions == null) {
        factoryOptions = WorkerFactoryOptions.newBuilder().setDisableStickyExecution(false).build();
      }

      return new TestEnvironmentOptions(
          dataConverter,
          interceptorFactory,
          factoryOptions,
          workflowClientOptions,
          enableLoggingInReplay);
    }
  }

  private final DataConverter dataConverter;
  private final Function<WorkflowInterceptor, WorkflowInterceptor> interceptorFactory;
  private final boolean enableLoggingInReplay;
  private final WorkerFactoryOptions workerFactoryOptions;
  private final WorkflowClientOptions workflowClientOptions;

  private TestEnvironmentOptions(
      DataConverter dataConverter,
      Function<WorkflowInterceptor, WorkflowInterceptor> interceptorFactory,
      WorkerFactoryOptions options,
      WorkflowClientOptions workflowClientOptions,
      boolean enableLoggingInReplay) {
    this.dataConverter = dataConverter;
    this.interceptorFactory = interceptorFactory;
    this.workerFactoryOptions = options;
    this.workflowClientOptions = workflowClientOptions;
    this.enableLoggingInReplay = enableLoggingInReplay;
  }

  public DataConverter getDataConverter() {
    return dataConverter;
  }

  public Function<WorkflowInterceptor, WorkflowInterceptor> getInterceptorFactory() {
    return interceptorFactory;
  }

  public boolean isLoggingEnabledInReplay() {
    return enableLoggingInReplay;
  }

  public WorkerFactoryOptions getWorkerFactoryOptions() {
    return workerFactoryOptions;
  }

  public WorkflowClientOptions getWorkflowClientOptions() {
    return workflowClientOptions;
  }

  @Override
  public String toString() {
    return "TestEnvironmentOptions{"
        + "dataConverter="
        + dataConverter
        + ", workerFactoryOptions="
        + workerFactoryOptions
        + ", workflowClientOptions="
        + workflowClientOptions
        + '}';
  }
}
