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
package com.uber.cadence.internal.shadowing;

/**
 * This class is the JSON serializable class of {@link com.uber.cadence.WorkflowExecution} Make sure
 * this class is sync with auto generated WorkflowExecution
 */
public class WorkflowExecution {
  private String workflowId;
  private String runId;

  public WorkflowExecution() {}

  public WorkflowExecution(com.uber.cadence.WorkflowExecution workflowExecution) {
    this.workflowId = workflowExecution.getWorkflowId();
    this.runId = workflowExecution.getRunId();
  }

  public WorkflowExecution(String workflowId, String runId) {
    this.workflowId = workflowId;
    this.runId = runId;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getRunId() {
    return runId;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }

  public com.uber.cadence.WorkflowExecution toEntity() {
    return new com.uber.cadence.WorkflowExecution().setWorkflowId(workflowId).setRunId(runId);
  }

  @Override
  public String toString() {
    return "WorkflowExecution{"
        + "workflowId='"
        + workflowId
        + '\''
        + ", runId='"
        + runId
        + '\''
        + '}';
  }
}
