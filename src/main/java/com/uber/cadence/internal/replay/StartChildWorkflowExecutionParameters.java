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

package com.uber.cadence.internal.replay;

import com.uber.cadence.ActiveClusterSelectionPolicy;
import com.uber.cadence.CronOverlapPolicy;
import com.uber.cadence.ParentClosePolicy;
import com.uber.cadence.WorkflowIdReusePolicy;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.internal.common.RetryParameters;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public final class StartChildWorkflowExecutionParameters {

  public static final class Builder {

    private String domain;

    private String control;

    private long executionStartToCloseTimeoutSeconds;

    private byte[] input;

    private String taskList;

    private long taskStartToCloseTimeoutSeconds;

    private String workflowId;

    private WorkflowType workflowType;

    private WorkflowIdReusePolicy workflowIdReusePolicy;

    private RetryParameters retryParameters;

    private String cronSchedule;

    private Map<String, Object> memo;

    private Map<String, Object> searchAttributes;

    private Map<String, byte[]> context;

    private ParentClosePolicy parentClosePolicy;

    private CronOverlapPolicy cronOverlapPolicy;

    private ActiveClusterSelectionPolicy activeClusterSelectionPolicy;

    public Builder setDomain(String domain) {
      this.domain = domain;
      return this;
    }

    public Builder setControl(String control) {
      this.control = control;
      return this;
    }

    public Builder setExecutionStartToCloseTimeoutSeconds(
        long executionStartToCloseTimeoutSeconds) {
      this.executionStartToCloseTimeoutSeconds = executionStartToCloseTimeoutSeconds;
      return this;
    }

    public Builder setInput(byte[] input) {
      this.input = input;
      return this;
    }

    public Builder setTaskList(String taskList) {
      this.taskList = taskList;
      return this;
    }

    public Builder setTaskStartToCloseTimeoutSeconds(long taskStartToCloseTimeoutSeconds) {
      this.taskStartToCloseTimeoutSeconds = taskStartToCloseTimeoutSeconds;
      return this;
    }

    public Builder setWorkflowId(String workflowId) {
      this.workflowId = workflowId;
      return this;
    }

    public Builder setWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public Builder setWorkflowIdReusePolicy(WorkflowIdReusePolicy workflowIdReusePolicy) {
      this.workflowIdReusePolicy = workflowIdReusePolicy;
      return this;
    }

    public Builder setRetryParameters(RetryParameters retryParameters) {
      this.retryParameters = retryParameters;
      return this;
    }

    public Builder setCronSchedule(String cronSchedule) {
      this.cronSchedule = cronSchedule;
      return this;
    }

    public Builder setMemo(Map<String, Object> memo) {
      this.memo = memo;
      return this;
    }

    public Builder setSearchAttributes(Map<String, Object> searchAttributes) {
      this.searchAttributes = searchAttributes;
      return this;
    }

    public Builder setContext(Map<String, byte[]> context) {
      this.context = context;
      return this;
    }

    public Builder setParentClosePolicy(ParentClosePolicy parentClosePolicy) {
      this.parentClosePolicy = parentClosePolicy;
      return this;
    }

    public Builder setCronOverlapPolicy(CronOverlapPolicy cronOverlapPolicy) {
      this.cronOverlapPolicy = cronOverlapPolicy;
      return this;
    }

    public Builder setActiveClusterSelectionPolicy(
        ActiveClusterSelectionPolicy activeClusterSelectionPolicy) {
      this.activeClusterSelectionPolicy = activeClusterSelectionPolicy;
      return this;
    }

    public StartChildWorkflowExecutionParameters build() {
      return new StartChildWorkflowExecutionParameters(
          domain,
          input,
          control,
          executionStartToCloseTimeoutSeconds,
          taskList,
          taskStartToCloseTimeoutSeconds,
          workflowId,
          workflowType,
          workflowIdReusePolicy,
          retryParameters,
          cronSchedule,
          memo,
          searchAttributes,
          context,
          parentClosePolicy,
          cronOverlapPolicy,
          activeClusterSelectionPolicy);
    }
  }

  private final String domain;

  private final String control;

  private final long executionStartToCloseTimeoutSeconds;

  private final byte[] input;

  private final String taskList;

  private final long taskStartToCloseTimeoutSeconds;

  private final String workflowId;

  private final WorkflowType workflowType;

  private final WorkflowIdReusePolicy workflowIdReusePolicy;

  private final RetryParameters retryParameters;

  private final String cronSchedule;

  private Map<String, Object> memo;

  private Map<String, Object> searchAttributes;

  private Map<String, byte[]> context;

  private final ParentClosePolicy parentClosePolicy;

  private final CronOverlapPolicy cronOverlapPolicy;

  private final ActiveClusterSelectionPolicy activeClusterSelectionPolicy;

  private StartChildWorkflowExecutionParameters(
      String domain,
      byte[] input,
      String control,
      long executionStartToCloseTimeoutSeconds,
      String taskList,
      long taskStartToCloseTimeoutSeconds,
      String workflowId,
      WorkflowType workflowType,
      WorkflowIdReusePolicy workflowIdReusePolicy,
      RetryParameters retryParameters,
      String cronSchedule,
      Map<String, Object> memo,
      Map<String, Object> searchAttributes,
      Map<String, byte[]> context,
      ParentClosePolicy parentClosePolicy,
      CronOverlapPolicy cronOverlapPolicy,
      ActiveClusterSelectionPolicy activeClusterSelectionPolicy) {
    this.domain = domain;
    this.input = input;
    this.control = control;
    this.executionStartToCloseTimeoutSeconds = executionStartToCloseTimeoutSeconds;
    this.taskList = taskList;
    this.taskStartToCloseTimeoutSeconds = taskStartToCloseTimeoutSeconds;
    this.workflowId = workflowId;
    this.workflowType = workflowType;
    this.workflowIdReusePolicy = workflowIdReusePolicy;
    this.retryParameters = retryParameters;
    this.cronSchedule = cronSchedule;
    this.memo = memo;
    this.searchAttributes = searchAttributes;
    this.context = context;
    this.parentClosePolicy = parentClosePolicy;
    this.cronOverlapPolicy = cronOverlapPolicy;
    this.activeClusterSelectionPolicy = activeClusterSelectionPolicy;
  }

  public String getDomain() {
    return domain;
  }

  public String getControl() {
    return control;
  }

  public long getExecutionStartToCloseTimeoutSeconds() {
    return executionStartToCloseTimeoutSeconds;
  }

  public byte[] getInput() {
    return input;
  }

  public String getTaskList() {
    return taskList;
  }

  public long getTaskStartToCloseTimeoutSeconds() {
    return taskStartToCloseTimeoutSeconds;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  public WorkflowIdReusePolicy getWorkflowIdReusePolicy() {
    return workflowIdReusePolicy;
  }

  public RetryParameters getRetryParameters() {
    return retryParameters;
  }

  public String getCronSchedule() {
    return cronSchedule;
  }

  public Map<String, Object> getMemo() {
    return memo;
  }

  public Map<String, Object> getSearchAttributes() {
    return searchAttributes;
  }

  public Map<String, byte[]> getContext() {
    return context;
  }

  public ParentClosePolicy getParentClosePolicy() {
    return parentClosePolicy;
  }

  public CronOverlapPolicy getCronOverlapPolicy() {
    return cronOverlapPolicy;
  }

  public ActiveClusterSelectionPolicy getActiveClusterSelectionPolicy() {
    return activeClusterSelectionPolicy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StartChildWorkflowExecutionParameters that = (StartChildWorkflowExecutionParameters) o;
    return executionStartToCloseTimeoutSeconds == that.executionStartToCloseTimeoutSeconds
        && taskStartToCloseTimeoutSeconds == that.taskStartToCloseTimeoutSeconds
        && Objects.equals(domain, that.domain)
        && Objects.equals(control, that.control)
        && Arrays.equals(input, that.input)
        && Objects.equals(taskList, that.taskList)
        && Objects.equals(workflowId, that.workflowId)
        && Objects.equals(workflowType, that.workflowType)
        && workflowIdReusePolicy == that.workflowIdReusePolicy
        && Objects.equals(retryParameters, that.retryParameters)
        && Objects.equals(cronSchedule, that.cronSchedule)
        && Objects.equals(memo, that.memo)
        && Objects.equals(searchAttributes, that.searchAttributes)
        && Objects.equals(context, that.context)
        && Objects.equals(parentClosePolicy, that.parentClosePolicy)
        && cronOverlapPolicy == that.cronOverlapPolicy
        && Objects.equals(activeClusterSelectionPolicy, that.activeClusterSelectionPolicy);
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(
            domain,
            control,
            executionStartToCloseTimeoutSeconds,
            taskList,
            taskStartToCloseTimeoutSeconds,
            workflowId,
            workflowType,
            workflowIdReusePolicy,
            retryParameters,
            cronSchedule,
            memo,
            searchAttributes,
            context,
            parentClosePolicy,
            cronOverlapPolicy,
            activeClusterSelectionPolicy);
    result = 31 * result + Arrays.hashCode(input);
    return result;
  }

  @Override
  public String toString() {
    return "StartChildWorkflowExecutionParameters{"
        + "domain='"
        + domain
        + '\''
        + ", control='"
        + control
        + '\''
        + ", executionStartToCloseTimeoutSeconds="
        + executionStartToCloseTimeoutSeconds
        + ", input="
        + Arrays.toString(input)
        + ", taskList='"
        + taskList
        + '\''
        + ", taskStartToCloseTimeoutSeconds="
        + taskStartToCloseTimeoutSeconds
        + ", workflowId='"
        + workflowId
        + '\''
        + ", workflowType="
        + workflowType
        + ", workflowIdReusePolicy="
        + workflowIdReusePolicy
        + ", retryParameters="
        + retryParameters
        + ", cronSchedule="
        + cronSchedule
        + ", memo="
        + memo
        + ", searchAttributes="
        + searchAttributes
        + ", context='"
        + context
        + ", parentClosePolicy="
        + parentClosePolicy
        + ", cronOverlapPolicy="
        + cronOverlapPolicy
        + ", activeClusterSelectionPolicy="
        + activeClusterSelectionPolicy
        + '}';
  }
}
