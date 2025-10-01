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

package com.uber.cadence.workflow;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionStartedEventAttributes;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.activity.LocalActivityOptions;
import com.uber.cadence.internal.sync.SyncWorkflowDefinition;
import com.uber.cadence.internal.worker.WorkflowExecutionException;
import com.uber.cadence.workflow.Functions.Func;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public interface WorkflowInterceptor {

  public final class WorkflowExecuteInput {
    private final WorkflowExecutionStartedEventAttributes workflowEventStart;
    private final WorkflowType workflowType;
    private final byte[] input;

    public WorkflowExecuteInput(WorkflowExecutionStartedEventAttributes workflowEventStart) {
      this.workflowEventStart = workflowEventStart;
      this.workflowType = workflowEventStart.getWorkflowType();
      this.input = workflowEventStart.getInput();
    }

    public WorkflowType getWorkflowType() {
      return workflowType;
    }

    public byte[] getInput() {
      return input;
    }

    public WorkflowExecutionStartedEventAttributes getWorkflowExecutionStartedEventAttributes() {
      return workflowEventStart;
    }
  }

  final class WorkflowResult<R> {

    private final Promise<R> result;
    private final Promise<WorkflowExecution> workflowExecution;

    public WorkflowResult(Promise<R> result, Promise<WorkflowExecution> workflowExecution) {
      this.result = result;
      this.workflowExecution = workflowExecution;
    }

    public Promise<R> getResult() {
      return result;
    }

    public Promise<WorkflowExecution> getWorkflowExecution() {
      return workflowExecution;
    }
  }

  // to match behavior in go client: interceptor executeWorkflow method
  byte[] executeWorkflow(SyncWorkflowDefinition workflowDefinition, WorkflowExecuteInput input)
      throws CancellationException, WorkflowExecutionException;

  <R> Promise<R> executeActivity(
      String activityName,
      Class<R> resultClass,
      Type resultType,
      Object[] args,
      ActivityOptions options);

  <R> Promise<R> executeLocalActivity(
      String activityName,
      Class<R> resultClass,
      Type resultType,
      Object[] args,
      LocalActivityOptions options);

  <R> WorkflowResult<R> executeChildWorkflow(
      String workflowType,
      Class<R> resultClass,
      Type resultType,
      Object[] args,
      ChildWorkflowOptions options);

  Random newRandom();

  Promise<Void> signalExternalWorkflow(
      WorkflowExecution execution, String signalName, Object[] args);

  Promise<Void> signalExternalWorkflow(
      String domain, WorkflowExecution execution, String signalName, Object[] args);

  Promise<Void> cancelWorkflow(WorkflowExecution execution);

  void sleep(Duration duration);

  boolean await(Duration timeout, String reason, Supplier<Boolean> unblockCondition);

  void await(String reason, Supplier<Boolean> unblockCondition);

  Promise<Void> newTimer(Duration duration);

  <R> R sideEffect(Class<R> resultClass, Type resultType, Func<R> func);

  <R> R mutableSideEffect(
      String id, Class<R> resultClass, Type resultType, BiPredicate<R, R> updated, Func<R> func);

  int getVersion(String changeID, int minSupported, int maxSupported);

  /**
   * Enhanced version of getVersion with additional options for version control.
   *
   * @param changeID identifier of a particular change
   * @param minSupported min version supported for the change
   * @param maxSupported max version supported for the change
   * @param options version control options
   * @return version
   */
  int getVersion(String changeID, int minSupported, int maxSupported, GetVersionOptions options);

  void continueAsNew(
      Optional<String> workflowType, Optional<ContinueAsNewOptions> options, Object[] args);

  void registerQuery(String queryType, Type[] argTypes, Functions.Func1<Object[], Object> callback);

  UUID randomUUID();

  void upsertSearchAttributes(Map<String, Object> searchAttributes);
}
