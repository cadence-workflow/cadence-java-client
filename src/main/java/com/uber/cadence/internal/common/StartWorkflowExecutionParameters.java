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

package com.uber.cadence.internal.common;

import com.uber.cadence.ChildPolicy;
import com.uber.cadence.WorkflowIdReusePolicy;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.common.RetryOptions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class StartWorkflowExecutionParameters {

  private String workflowId;

  private WorkflowType workflowType;

  private String taskList;

  private byte[] input;

  private long executionStartToCloseTimeoutSeconds;

  private long taskStartToCloseTimeoutSeconds;

  private ChildPolicy childPolicy;

  private WorkflowIdReusePolicy workflowIdReusePolicy;

  private RetryParameters retryParameters;

  /**
   * Returns the value of the WorkflowId property for this object.
   *
   * <p><b>Constraints:</b><br>
   * <b>Length: </b>1 - 64<br>
   *
   * @return The value of the WorkflowId property for this object.
   */
  public String getWorkflowId() {
    return workflowId;
  }

  /**
   * Sets the value of the WorkflowId property for this object.
   *
   * <p><b>Constraints:</b><br>
   * <b>Length: </b>1 - 64<br>
   *
   * @param workflowId The new value for the WorkflowId property for this object.
   */
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  /**
   * Sets the value of the WorkflowId property for this object.
   *
   * <p>Returns a reference to this object so that method calls can be chained together.
   *
   * <p><b>Constraints:</b><br>
   * <b>Length: </b>1 - 64<br>
   *
   * @param workflowId The new value for the WorkflowId property for this object.
   * @return A reference to this updated object so that method calls can be chained together.
   */
  public StartWorkflowExecutionParameters withWorkflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * Returns the value of the WorkflowType property for this object.
   *
   * @return The value of the WorkflowType property for this object.
   */
  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  /**
   * Sets the value of the WorkflowType property for this object.
   *
   * @param workflowType The new value for the WorkflowType property for this object.
   */
  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  /**
   * Sets the value of the WorkflowType property for this object.
   *
   * <p>Returns a reference to this object so that method calls can be chained together.
   *
   * @param workflowType The new value for the WorkflowType property for this object.
   * @return A reference to this updated object so that method calls can be chained together.
   */
  public StartWorkflowExecutionParameters withWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
    return this;
  }

  public WorkflowIdReusePolicy getWorkflowIdReusePolicy() {
    return workflowIdReusePolicy;
  }

  public void setWorkflowIdReusePolicy(WorkflowIdReusePolicy workflowIdReusePolicy) {
    this.workflowIdReusePolicy = workflowIdReusePolicy;
  }

  public StartWorkflowExecutionParameters withWorkflowIdReusePolicy(
      WorkflowIdReusePolicy workflowIdReusePolicy) {
    this.workflowIdReusePolicy = workflowIdReusePolicy;
    return this;
  }

  /**
   * Returns the value of the TaskList property for this object.
   *
   * @return The value of the TaskList property for this object.
   */
  public String getTaskList() {
    return taskList;
  }

  /**
   * Sets the value of the TaskList property for this object.
   *
   * @param taskList The new value for the TaskList property for this object.
   */
  public void setTaskList(String taskList) {
    this.taskList = taskList;
  }

  /**
   * Sets the value of the TaskList property for this object.
   *
   * <p>Returns a reference to this object so that method calls can be chained together.
   *
   * @param taskList The new value for the TaskList property for this object.
   * @return A reference to this updated object so that method calls can be chained together.
   */
  public StartWorkflowExecutionParameters withTaskList(String taskList) {
    this.taskList = taskList;
    return this;
  }

  /**
   * Returns the value of the Input property for this object.
   *
   * <p><b>Constraints:</b><br>
   * <b>Length: </b>0 - 100000<br>
   *
   * @return The value of the Input property for this object.
   */
  public byte[] getInput() {
    return input;
  }

  /**
   * Sets the value of the Input property for this object.
   *
   * <p><b>Constraints:</b><br>
   * <b>Length: </b>0 - 100000<br>
   *
   * @param input The new value for the Input property for this object.
   */
  public void setInput(byte[] input) {
    this.input = input;
  }

  /**
   * Sets the value of the Input property for this object.
   *
   * <p>Returns a reference to this object so that method calls can be chained together.
   *
   * <p><b>Constraints:</b><br>
   * <b>Length: </b>0 - 100000<br>
   *
   * @param input The new value for the Input property for this object.
   * @return A reference to this updated object so that method calls can be chained together.
   */
  public StartWorkflowExecutionParameters withInput(byte[] input) {
    this.input = input;
    return this;
  }

  /**
   * Returns the value of the StartToCloseTimeout property for this object.
   *
   * <p><b>Constraints:</b><br>
   * <b>Length: </b>0 - 64<br>
   *
   * @return The value of the StartToCloseTimeout property for this object.
   */
  public long getExecutionStartToCloseTimeoutSeconds() {
    return executionStartToCloseTimeoutSeconds;
  }

  /**
   * Sets the value of the StartToCloseTimeout property for this object.
   *
   * <p><b>Constraints:</b><br>
   * <b>Length: </b>0 - 64<br>
   *
   * @param executionStartToCloseTimeoutSeconds The new value for the StartToCloseTimeout property
   *     for this object.
   */
  public void setExecutionStartToCloseTimeoutSeconds(long executionStartToCloseTimeoutSeconds) {
    this.executionStartToCloseTimeoutSeconds = executionStartToCloseTimeoutSeconds;
  }

  /**
   * Sets the value of the StartToCloseTimeout property for this object.
   *
   * <p>Returns a reference to this object so that method calls can be chained together.
   *
   * <p><b>Constraints:</b><br>
   * <b>Length: </b>0 - 64<br>
   *
   * @param executionStartToCloseTimeoutSeconds The new value for the StartToCloseTimeout property
   *     for this object.
   * @return A reference to this updated object so that method calls can be chained together.
   */
  public StartWorkflowExecutionParameters withExecutionStartToCloseTimeoutSeconds(
      long executionStartToCloseTimeoutSeconds) {
    this.executionStartToCloseTimeoutSeconds = executionStartToCloseTimeoutSeconds;
    return this;
  }

  public long getTaskStartToCloseTimeoutSeconds() {
    return taskStartToCloseTimeoutSeconds;
  }

  public void setTaskStartToCloseTimeoutSeconds(long taskStartToCloseTimeoutSeconds) {
    this.taskStartToCloseTimeoutSeconds = taskStartToCloseTimeoutSeconds;
  }

  public StartWorkflowExecutionParameters withTaskStartToCloseTimeoutSeconds(
      int taskStartToCloseTimeoutSeconds) {
    this.taskStartToCloseTimeoutSeconds = taskStartToCloseTimeoutSeconds;
    return this;
  }

  public ChildPolicy getChildPolicy() {
    return childPolicy;
  }

  public void setChildPolicy(ChildPolicy childPolicy) {
    this.childPolicy = childPolicy;
  }

  public StartWorkflowExecutionParameters withChildPolicy(ChildPolicy childPolicy) {
    this.childPolicy = childPolicy;
    return this;
  }

  public RetryParameters getRetryParameters() {
    return retryParameters;
  }

  public void setRetryParameters(RetryParameters retryParameters) {
    this.retryParameters = retryParameters;
  }

  public StartWorkflowExecutionParameters withRetryParameters(RetryParameters retryParameters) {
    this.retryParameters = retryParameters;
    return this;
  }

  public static StartWorkflowExecutionParameters createStartWorkflowExecutionParametersFromOptions(
      WorkflowOptions options) {
    StartWorkflowExecutionParameters parameters = new StartWorkflowExecutionParameters();
    parameters.setExecutionStartToCloseTimeoutSeconds(
        (int) getSeconds(options.getExecutionStartToCloseTimeout()));
    parameters.setTaskStartToCloseTimeoutSeconds(
        (int) getSeconds(options.getTaskStartToCloseTimeout()));
    parameters.setTaskList(options.getTaskList());
    parameters.setChildPolicy(options.getChildPolicy());
    RetryOptions retryOptions = options.getRetryOptions();
    if (retryOptions != null) {
      RetryParameters rp = new RetryParameters();
      rp.setBackoffCoefficient(retryOptions.getBackoffCoefficient());
      rp.setExpirationIntervalInSeconds((int) getSeconds(retryOptions.getExpiration()));
      rp.setInitialIntervalInSeconds((int) getSeconds(retryOptions.getInitialInterval()));
      rp.setMaximumIntervalInSeconds((int) getSeconds(retryOptions.getMaximumInterval()));
      rp.setMaximumAttempts(retryOptions.getMaximumAttempts());
      List<String> reasons = new ArrayList<>();
      // Use exception type name as the reason
      for (Class<? extends Throwable> r : retryOptions.getDoNotRetry()) {
        reasons.add(r.getName());
      }
      rp.setNonRetriableErrorReasons(reasons);
      parameters.setRetryParameters(rp);
    }
    return parameters;
  }

  private static long getSeconds(Duration expiration) {
    if (expiration == null) {
      return 0;
    }
    return expiration.getSeconds();
  }

  public StartWorkflowExecutionParameters copy() {
    StartWorkflowExecutionParameters result = new StartWorkflowExecutionParameters();
    result.setInput(input);
    result.setExecutionStartToCloseTimeoutSeconds(executionStartToCloseTimeoutSeconds);
    result.setTaskStartToCloseTimeoutSeconds(taskStartToCloseTimeoutSeconds);
    result.setTaskList(taskList);
    result.setWorkflowId(workflowId);
    result.setWorkflowType(workflowType);
    result.setChildPolicy(childPolicy);
    result.setRetryParameters(retryParameters.copy());
    return result;
  }

  @Override
  public String toString() {
    return "StartWorkflowExecutionParameters{"
        + "workflowId='"
        + workflowId
        + '\''
        + ", workflowType="
        + workflowType
        + ", taskList='"
        + taskList
        + '\''
        + ", input.length="
        + input.length
        + ", executionStartToCloseTimeoutSeconds="
        + executionStartToCloseTimeoutSeconds
        + ", taskStartToCloseTimeoutSeconds="
        + taskStartToCloseTimeoutSeconds
        + ", childPolicy="
        + childPolicy
        + ", workflowIdReusePolicy="
        + workflowIdReusePolicy
        + ", retryParameters="
        + retryParameters
        + '}';
  }
}
