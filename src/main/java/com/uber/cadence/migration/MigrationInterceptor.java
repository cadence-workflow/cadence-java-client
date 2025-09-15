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

package com.uber.cadence.migration;

import com.google.common.base.Strings;
import com.uber.cadence.*;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.internal.sync.SyncWorkflowDefinition;
import com.uber.cadence.workflow.*;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class MigrationInterceptor extends WorkflowInterceptorBase {
  private final WorkflowInterceptor next;
  private final String domainNew;
  private WorkflowClient clientInNewDomain;

  private static final String versionChangeID = "cadenceMigrationInterceptor";
  private static final int versionV1 = 1;
  private final ActivityOptions activityOptions =
      new ActivityOptions.Builder().setScheduleToCloseTimeout(Duration.ofSeconds(10)).build();

  private class MigrationDecision {
    boolean shouldMigrate;
    String reason;

    public MigrationDecision(boolean shouldMigrate, String reason) {
      this.shouldMigrate = shouldMigrate;
      this.reason = reason;
    }
  }

  public MigrationInterceptor(WorkflowInterceptor next, WorkflowClient clientInNewDomain) {
    super(next);
    this.next = next;
    this.domainNew = clientInNewDomain.getOptions().getDomain();
  }

  /**
   * MigrationInterceptor intercept executeWorkflow method to identify cron scheduled workflows.
   *
   * <p>Steps to migrate a cron workflow: 1. Identify cron and non-child workflows from start event
   * 2. Start execution in the new domain 3. Cancel current workflow execution 4. If anything
   * failed, fallback to cron workflow execution
   *
   * <p>If successful, the current cron workflow should be canceled with migration reason and a new
   * workflow execution with the same workflow-id and input should start in the new domain.
   *
   * <p>WARNING: it's possible to have both workflows running at the same time, if cancel step
   * failed.
   *
   * @param workflowDefinition
   * @param input
   * @return workflow result
   */
  @Override
  public byte[] executeWorkflow(
      SyncWorkflowDefinition workflowDefinition, WorkflowExecuteInput input) {

    WorkflowInfo workflowInfo = Workflow.getWorkflowInfo();
    // Versioning to ensure replay is deterministic
    int version = getVersion(versionChangeID, Workflow.DEFAULT_VERSION, versionV1);
    switch (version) {
      case versionV1:
        // Skip migration on non-cron and child workflows
        WorkflowExecutionStartedEventAttributes startedEventAttributes =
            input.getWorkflowExecutionStartedEventAttributes();
        if (!isCronSchedule(startedEventAttributes))
          return next.executeWorkflow(workflowDefinition, input);
        if (isChildWorkflow(startedEventAttributes)) {
          return next.executeWorkflow(workflowDefinition, input);
        }

        // deterministically make migration decision by a SideEffect
        MigrationDecision decision =
            Workflow.sideEffect(
                MigrationDecision.class, () -> shouldMigrate(workflowDefinition, input));
        if (decision.shouldMigrate) {
          MigrationActivities activities =
              Workflow.newActivityStub(MigrationActivities.class, activityOptions);
          try {
            // start new workflow in new domain
            activities.startWorkflowInNewDomain(
                new StartWorkflowExecutionRequest()
                    .setDomain(domainNew)
                    .setWorkflowId(workflowInfo.getWorkflowId())
                    .setTaskList(
                        new TaskList().setName(startedEventAttributes.getTaskList().getName()))
                    .setInput(input.getInput())
                    .setWorkflowType(new WorkflowType().setName(input.getWorkflowType().getName()))
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.TerminateIfRunning)
                    .setRetryPolicy(startedEventAttributes.getRetryPolicy())
                    .setRequestId(UUID.randomUUID().toString())
                    .setIdentity(startedEventAttributes.getIdentity())
                    .setMemo(startedEventAttributes.getMemo())
                    .setCronSchedule(startedEventAttributes.getCronSchedule())
                    .setHeader(startedEventAttributes.getHeader())
                    .setSearchAttributes(startedEventAttributes.getSearchAttributes())
                    .setExecutionStartToCloseTimeoutSeconds(
                        startedEventAttributes.getExecutionStartToCloseTimeoutSeconds())
                    .setTaskStartToCloseTimeoutSeconds(
                        startedEventAttributes.getTaskStartToCloseTimeoutSeconds()));

            // cancel current workflow
            cancelCurrentWorkflow();
          } catch (ActivityException e) {
            // fallback if start workflow in new domain failed
            return next.executeWorkflow(workflowDefinition, input);
          }
        }
      default:
        return next.executeWorkflow(workflowDefinition, input);
    }
  }

  private MigrationDecision shouldMigrate(
      SyncWorkflowDefinition workflowDefinition, WorkflowExecuteInput input) {
    return new MigrationDecision(true, "");
  }

  /**
   * MigrationInterceptor intercepts continueAsNew method to migrate workflows that explicitly wants
   * to continue as new.
   *
   * <p>Steps to migrate a continue-as-new workflow: 1. workflow execution is already finished and
   * is about to continue as new 2.
   *
   * <p>NOTE: For cron workflows, this method will NOT be called but is handled by executeWorkflow
   *
   * <p>WARNING: Like cron-workflow migration, it's possible to have two continue-as-new workflows
   * running in two domains if cancellation fails.
   *
   * @param workflowType
   * @param options
   * @param args
   */
  @Override
  public void continueAsNew(
      Optional<String> workflowType, Optional<ContinueAsNewOptions> options, Object[] args) {

    // Versioning to ensure replay is deterministic
    int version = getVersion(versionChangeID, Workflow.DEFAULT_VERSION, versionV1);
    switch (version) {
      case versionV1:
        WorkflowInfo workflowInfo = Workflow.getWorkflowInfo();
        WorkflowExecutionStartedEventAttributes startedEventAttributes =
            workflowInfo.getWorkflowExecutionStartedEventAttributes();
        if (isChildWorkflow(startedEventAttributes)) {
          next.continueAsNew(workflowType, options, args);
        }
        MigrationDecision decision =
            Workflow.sideEffect(MigrationDecision.class, () -> new MigrationDecision(true, ""));
        if (decision.shouldMigrate) {
          try {
            MigrationActivities activities =
                Workflow.newActivityStub(MigrationActivities.class, activityOptions);
            activities.startWorkflowInNewDomain(
                new StartWorkflowExecutionRequest()
                    .setDomain(domainNew)
                    .setWorkflowId(workflowInfo.getWorkflowId())
                    .setTaskList(
                        new TaskList().setName(startedEventAttributes.getTaskList().getName()))
                    .setInput(workflowInfo.getDataConverter().toData(args))
                    .setWorkflowType(
                        new WorkflowType()
                            .setName(startedEventAttributes.getWorkflowType().getName()))
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.TerminateIfRunning)
                    .setRetryPolicy(startedEventAttributes.getRetryPolicy())
                    .setRequestId(UUID.randomUUID().toString())
                    .setIdentity(startedEventAttributes.getIdentity())
                    .setMemo(startedEventAttributes.getMemo())
                    .setCronSchedule(startedEventAttributes.getCronSchedule())
                    .setHeader(startedEventAttributes.getHeader())
                    .setSearchAttributes(startedEventAttributes.getSearchAttributes())
                    .setExecutionStartToCloseTimeoutSeconds(
                        startedEventAttributes.getExecutionStartToCloseTimeoutSeconds())
                    .setTaskStartToCloseTimeoutSeconds(
                        startedEventAttributes.getTaskStartToCloseTimeoutSeconds()));
            cancelCurrentWorkflow();
          } catch (ActivityException e) {
            // fallback if start workflow in new domain failed
            next.continueAsNew(workflowType, options, args);
          }
        }
      default:
        next.continueAsNew(workflowType, options, args);
    }
  }

  private boolean isChildWorkflow(WorkflowExecutionStartedEventAttributes startedEventAttributes) {
    return startedEventAttributes.getParentWorkflowExecution() != null
        && startedEventAttributes.getParentWorkflowExecution().getWorkflowId() != null;
  }

  private boolean isCronSchedule(WorkflowExecutionStartedEventAttributes startedEventAttributes) {
    return !Strings.isNullOrEmpty(startedEventAttributes.getCronSchedule());
  }

  private void cancelCurrentWorkflow() {
    // a detached scope is needed otherwise there will be a race condition between
    // completion of activity and the workflow cancellation event
    WorkflowInfo workflowInfo = Workflow.getWorkflowInfo();
    MigrationActivities activities =
        Workflow.newActivityStub(MigrationActivities.class, activityOptions);
    Workflow.newDetachedCancellationScope(
            () -> {
              activities.cancelWorkflowInCurrentDomain(
                  new RequestCancelWorkflowExecutionRequest()
                      .setDomain(workflowInfo.getDomain())
                      .setWorkflowExecution(
                          new WorkflowExecution()
                              .setWorkflowId(workflowInfo.getWorkflowId())
                              .setRunId(workflowInfo.getRunId())));
            })
        .run();
  }
}
