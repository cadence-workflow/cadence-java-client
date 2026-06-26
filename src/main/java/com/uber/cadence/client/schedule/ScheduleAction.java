/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.client.schedule;

import com.uber.cadence.common.RetryOptions;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines what the schedule does when it fires: starts a new workflow execution.
 *
 * <p>Currently the only supported action is {@link StartWorkflowAction}; the outer {@link
 * ScheduleAction} wrapper is kept to allow additional action types in the future.
 *
 * <p>Construct via {@link #newBuilder()}:
 *
 * <pre>{@code
 * ScheduleAction action = ScheduleAction.newBuilder()
 *     .setStartWorkflow(StartWorkflowAction.newBuilder()
 *         .setWorkflowType("MyWorkflow")
 *         .setTaskList("my-task-list")
 *         .setExecutionStartToCloseTimeout(Duration.ofHours(1))
 *         .build())
 *     .build();
 * }</pre>
 */
public final class ScheduleAction {

  private final StartWorkflowAction startWorkflow;

  private ScheduleAction(Builder b) {
    this.startWorkflow = b.startWorkflow;
  }

  /** The workflow to start on each trigger. Exactly one action field must be non-null. */
  public StartWorkflowAction getStartWorkflow() {
    return startWorkflow;
  }

  /** Returns a new builder pre-populated with the values from this instance. */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /** Returns a new empty builder. */
  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScheduleAction)) return false;
    ScheduleAction that = (ScheduleAction) o;
    return Objects.equals(startWorkflow, that.startWorkflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startWorkflow);
  }

  @Override
  public String toString() {
    return "ScheduleAction{startWorkflow=" + startWorkflow + '}';
  }

  public static final class Builder {

    private StartWorkflowAction startWorkflow;

    private Builder() {}

    private Builder(ScheduleAction src) {
      this.startWorkflow = src.startWorkflow;
    }

    /** @see ScheduleAction#getStartWorkflow() */
    public Builder setStartWorkflow(StartWorkflowAction startWorkflow) {
      this.startWorkflow = startWorkflow;
      return this;
    }

    public ScheduleAction build() {
      if (startWorkflow == null) {
        throw new IllegalStateException("ScheduleAction requires startWorkflow to be set");
      }
      return new ScheduleAction(this);
    }
  }

  /**
   * Configures the workflow execution started on each schedule trigger.
   *
   * <p>Construct via {@link #newBuilder()}.
   */
  public static final class StartWorkflowAction {

    private final String workflowType;
    private final String taskList;
    private final byte[] input;
    private final String workflowIdPrefix;
    private final Duration executionStartToCloseTimeout;
    private final Duration taskStartToCloseTimeout;
    private final RetryOptions retryOptions;
    private final Map<String, Object> memo;
    private final Map<String, Object> searchAttributes;

    private StartWorkflowAction(Builder b) {
      this.workflowType = b.workflowType;
      this.taskList = b.taskList;
      this.input = b.input == null ? null : b.input.clone();
      this.workflowIdPrefix = b.workflowIdPrefix;
      this.executionStartToCloseTimeout = b.executionStartToCloseTimeout;
      this.taskStartToCloseTimeout = b.taskStartToCloseTimeout;
      this.retryOptions = b.retryOptions;
      this.memo = b.memo == null ? null : Collections.unmodifiableMap(new HashMap<>(b.memo));
      this.searchAttributes =
          b.searchAttributes == null
              ? null
              : Collections.unmodifiableMap(new HashMap<>(b.searchAttributes));
    }

    /** Workflow type (name) to start. */
    public String getWorkflowType() {
      return workflowType;
    }

    /** Task list on which the workflow decision tasks are dispatched. */
    public String getTaskList() {
      return taskList;
    }

    /** Serialized input payload for the workflow. {@code null} means no input. */
    public byte[] getInput() {
      return input == null ? null : input.clone();
    }

    /**
     * Prefix used when generating workflow IDs for triggered runs. The scheduler appends a
     * timestamp suffix to ensure uniqueness. {@code null} uses the schedule ID as prefix.
     */
    public String getWorkflowIdPrefix() {
      return workflowIdPrefix;
    }

    /** Maximum wall-clock time a triggered workflow execution may run. */
    public Duration getExecutionStartToCloseTimeout() {
      return executionStartToCloseTimeout;
    }

    /** Maximum time a single decision task may take. */
    public Duration getTaskStartToCloseTimeout() {
      return taskStartToCloseTimeout;
    }

    /** Retry policy applied to triggered workflow executions. {@code null} means no retries. */
    public RetryOptions getRetryOptions() {
      return retryOptions;
    }

    /** Memo key/value pairs attached to triggered workflow executions. */
    public Map<String, Object> getMemo() {
      return memo;
    }

    /** Search attributes attached to triggered workflow executions. */
    public Map<String, Object> getSearchAttributes() {
      return searchAttributes;
    }

    /** Returns a new builder pre-populated with the values from this instance. */
    public Builder toBuilder() {
      return new Builder(this);
    }

    /** Returns a new empty builder. */
    public static Builder newBuilder() {
      return new Builder();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof StartWorkflowAction)) return false;
      StartWorkflowAction that = (StartWorkflowAction) o;
      return Objects.equals(workflowType, that.workflowType)
          && Objects.equals(taskList, that.taskList)
          && Arrays.equals(input, that.input)
          && Objects.equals(workflowIdPrefix, that.workflowIdPrefix)
          && Objects.equals(executionStartToCloseTimeout, that.executionStartToCloseTimeout)
          && Objects.equals(taskStartToCloseTimeout, that.taskStartToCloseTimeout)
          && Objects.equals(retryOptions, that.retryOptions)
          && Objects.equals(memo, that.memo)
          && Objects.equals(searchAttributes, that.searchAttributes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          workflowType,
          taskList,
          Arrays.hashCode(input),
          workflowIdPrefix,
          executionStartToCloseTimeout,
          taskStartToCloseTimeout,
          retryOptions,
          memo,
          searchAttributes);
    }

    @Override
    public String toString() {
      return "StartWorkflowAction{"
          + "workflowType='"
          + workflowType
          + "', taskList='"
          + taskList
          + "', workflowIdPrefix='"
          + workflowIdPrefix
          + "', executionStartToCloseTimeout="
          + executionStartToCloseTimeout
          + '}';
    }

    public static final class Builder {

      private String workflowType;
      private String taskList;
      private byte[] input;
      private String workflowIdPrefix;
      private Duration executionStartToCloseTimeout;
      private Duration taskStartToCloseTimeout;
      private RetryOptions retryOptions;
      private Map<String, Object> memo;
      private Map<String, Object> searchAttributes;

      private Builder() {}

      private Builder(StartWorkflowAction src) {
        this.workflowType = src.workflowType;
        this.taskList = src.taskList;
        this.input = src.input;
        this.workflowIdPrefix = src.workflowIdPrefix;
        this.executionStartToCloseTimeout = src.executionStartToCloseTimeout;
        this.taskStartToCloseTimeout = src.taskStartToCloseTimeout;
        this.retryOptions = src.retryOptions;
        this.memo = src.memo;
        this.searchAttributes = src.searchAttributes;
      }

      /** @see StartWorkflowAction#getWorkflowType() */
      public Builder setWorkflowType(String workflowType) {
        this.workflowType = workflowType;
        return this;
      }

      /** @see StartWorkflowAction#getTaskList() */
      public Builder setTaskList(String taskList) {
        this.taskList = taskList;
        return this;
      }

      /** @see StartWorkflowAction#getInput() */
      public Builder setInput(byte[] input) {
        this.input = input == null ? null : input.clone();
        return this;
      }

      /** @see StartWorkflowAction#getWorkflowIdPrefix() */
      public Builder setWorkflowIdPrefix(String workflowIdPrefix) {
        this.workflowIdPrefix = workflowIdPrefix;
        return this;
      }

      /** @see StartWorkflowAction#getExecutionStartToCloseTimeout() */
      public Builder setExecutionStartToCloseTimeout(Duration executionStartToCloseTimeout) {
        this.executionStartToCloseTimeout = executionStartToCloseTimeout;
        return this;
      }

      /** @see StartWorkflowAction#getTaskStartToCloseTimeout() */
      public Builder setTaskStartToCloseTimeout(Duration taskStartToCloseTimeout) {
        this.taskStartToCloseTimeout = taskStartToCloseTimeout;
        return this;
      }

      /** @see StartWorkflowAction#getRetryOptions() */
      public Builder setRetryOptions(RetryOptions retryOptions) {
        this.retryOptions = retryOptions;
        return this;
      }

      /** @see StartWorkflowAction#getMemo() */
      public Builder setMemo(Map<String, Object> memo) {
        this.memo = memo;
        return this;
      }

      /** @see StartWorkflowAction#getSearchAttributes() */
      public Builder setSearchAttributes(Map<String, Object> searchAttributes) {
        this.searchAttributes = searchAttributes;
        return this;
      }

      public StartWorkflowAction build() {
        if (workflowType == null || workflowType.isEmpty()) {
          throw new IllegalStateException("StartWorkflowAction requires workflowType");
        }
        if (taskList == null || taskList.isEmpty()) {
          throw new IllegalStateException("StartWorkflowAction requires taskList");
        }
        return new StartWorkflowAction(this);
      }
    }
  }
}
