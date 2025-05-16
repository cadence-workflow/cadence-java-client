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
package com.uber.cadence.entities;

/** DecisionType represents the type of decision that can be made in a workflow. */
public enum DecisionType {
  /** Schedule an activity task. */
  ScheduleActivityTask,

  /** Request cancellation of an activity task. */
  RequestCancelActivityTask,

  /** Start a timer. */
  StartTimer,

  /** Complete the workflow execution. */
  CompleteWorkflowExecution,

  /** Fail the workflow execution. */
  FailWorkflowExecution,

  /** Cancel a timer. */
  CancelTimer,

  /** Cancel the workflow execution. */
  CancelWorkflowExecution,

  /** Request cancellation of an external workflow execution. */
  RequestCancelExternalWorkflowExecution,

  /** Record a marker in the workflow history. */
  RecordMarker,

  /** Continue the workflow execution as new. */
  ContinueAsNewWorkflowExecution,

  /** Start a child workflow execution. */
  StartChildWorkflowExecution,

  /** Signal an external workflow execution. */
  SignalExternalWorkflowExecution,

  /** Upsert workflow search attributes. */
  UpsertWorkflowSearchAttributes
}
