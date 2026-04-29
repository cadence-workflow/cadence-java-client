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

package com.uber.cadence.worker;

import com.google.common.base.Preconditions;
import java.time.Duration;

public class WorkerFactoryOptions {
  public static Builder newBuilder() {
    return new Builder();
  }

  private static final WorkerFactoryOptions DEFAULT_INSTANCE;
  private static final int DEFAULT_STICKY_CACHE_SIZE = 600;
  private static final int DEFAULT_MAX_WORKFLOW_THREAD_COUNT = 600;

  static {
    DEFAULT_INSTANCE = new Builder().build();
  }

  static WorkerFactoryOptions defaultInstance() {
    return DEFAULT_INSTANCE;
  }

  public static class Builder {
    private boolean disableStickyExecution;
    private int stickyCacheSize = DEFAULT_STICKY_CACHE_SIZE;
    private int maxWorkflowThreadCount = DEFAULT_MAX_WORKFLOW_THREAD_COUNT;
    private boolean enableLoggingInReplay;
    private ExecutorWrapper executorWrapper = ExecutorWrapper.newDefaultInstance();

    private Builder() {}

    /**
     * When set to false it will create an affinity between the worker and the workflow run it's
     * processing. Workers will cache workflows and will handle all decisions for that workflow
     * instance until it's complete or evicted from the cache. Default value is false.
     */
    public Builder setDisableStickyExecution(boolean disableStickyExecution) {
      this.disableStickyExecution = disableStickyExecution;
      return this;
    }

    /**
     * When Sticky execution is enabled this will set the maximum allowed number of workflows
     * cached. This cache is shared by all workers created by the Factory.
     *
     * <p>Default value is 600.
     */
    public Builder setStickyCacheSize(int stickyCacheSize) {
      this.stickyCacheSize = stickyCacheSize;
      return this;
    }

    /**
     * Maximum number of threads available for workflow execution across all workers created by the
     * Factory.
     *
     * <p>Default value is 600.
     */
    public Builder setMaxWorkflowThreadCount(int maxWorkflowThreadCount) {
      this.maxWorkflowThreadCount = maxWorkflowThreadCount;
      return this;
    }

    /**
     * Timeout for sticky workflow decision to be picked up by the host assigned to it. Once it
     * times out then it can be picked up by any worker. Default value is 5 seconds.
     *
     * @deprecated use {@link
     *     WorkerOptions.Builder#setStickyTaskListScheduleToStartTimeout(Duration)} to specify this
     *     value per-worker instead
     */
    @Deprecated
    public Builder setStickyTaskScheduleToStartTimeout(Duration stickyTaskScheduleToStartTimeout) {
      return this;
    }

    /**
     * PollerOptions for poller responsible for polling for decisions for workflows cached by all
     * workers created by this factory.
     */
    @Deprecated
    public Builder setStickyPollerCount(int stickyPollerCount) {
      return this;
    }

    public Builder setEnableLoggingInReplay(boolean enableLoggingInReplay) {
      this.enableLoggingInReplay = enableLoggingInReplay;
      return this;
    }

    public Builder setExecutorWrapper(ExecutorWrapper executorWrapper) {
      this.executorWrapper = executorWrapper;
      return this;
    }

    public WorkerFactoryOptions build() {
      return new WorkerFactoryOptions(
          disableStickyExecution,
          stickyCacheSize,
          maxWorkflowThreadCount,
          enableLoggingInReplay,
          executorWrapper);
    }
  }

  private final boolean disableStickyExecution;
  private final int cacheMaximumSize;
  private final int maxWorkflowThreadCount;
  private boolean enableLoggingInReplay;
  private int stickyPollerCount;
  private ExecutorWrapper executorWrapper;

  private WorkerFactoryOptions(
      boolean disableStickyExecution,
      int cacheMaximumSize,
      int maxWorkflowThreadCount,
      boolean enableLoggingInReplay,
      ExecutorWrapper executorWrapper) {
    Preconditions.checkArgument(cacheMaximumSize > 0, "cacheMaximumSize should be greater than 0");
    Preconditions.checkArgument(
        maxWorkflowThreadCount > 0, "maxWorkflowThreadCount should be greater than 0");

    this.disableStickyExecution = disableStickyExecution;
    this.cacheMaximumSize = cacheMaximumSize;
    this.maxWorkflowThreadCount = maxWorkflowThreadCount;
    this.enableLoggingInReplay = enableLoggingInReplay;
    this.executorWrapper = executorWrapper;
  }

  public int getMaxWorkflowThreadCount() {
    return maxWorkflowThreadCount;
  }

  public boolean isDisableStickyExecution() {
    return disableStickyExecution;
  }

  public int getCacheMaximumSize() {
    return cacheMaximumSize;
  }

  public boolean isEnableLoggingInReplay() {
    return enableLoggingInReplay;
  }

  @Deprecated
  public int getStickyPollerCount() {
    return 0;
  }

  @Deprecated
  public Duration getStickyTaskScheduleToStartTimeout() {
    return Duration.ZERO;
  }

  public ExecutorWrapper getExecutorWrapper() {
    return executorWrapper;
  }
}
