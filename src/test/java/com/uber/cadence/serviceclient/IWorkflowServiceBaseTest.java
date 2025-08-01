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
package com.uber.cadence.serviceclient;

import static org.junit.Assert.assertThrows;

import junit.framework.TestCase;

public class IWorkflowServiceBaseTest extends TestCase {

  public final IWorkflowServiceBase service = new IWorkflowServiceBase();

  public void testGetOptions() {
    assertThrows(UnsupportedOperationException.class, () -> service.getOptions());
  }

  public void testRegisterDomain() {
    assertThrows(UnsupportedOperationException.class, () -> service.RegisterDomain(null));
    assertThrows(UnsupportedOperationException.class, () -> service.RegisterDomain(null, null));
  }

  public void testDescribeDomain() {
    assertThrows(UnsupportedOperationException.class, () -> service.DescribeDomain(null));
    assertThrows(UnsupportedOperationException.class, () -> service.DescribeDomain(null, null));
  }

  public void testDiagnoseWorkflowExecution() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.DiagnoseWorkflowExecution(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.DiagnoseWorkflowExecution(null, null));
  }

  public void testListDomains() {
    assertThrows(UnsupportedOperationException.class, () -> service.ListDomains(null));
    assertThrows(UnsupportedOperationException.class, () -> service.ListDomains(null, null));
  }

  public void testUpdateDomain() {
    assertThrows(UnsupportedOperationException.class, () -> service.UpdateDomain(null));
    assertThrows(UnsupportedOperationException.class, () -> service.UpdateDomain(null, null));
  }

  public void testDeprecateDomain() {
    assertThrows(UnsupportedOperationException.class, () -> service.DeprecateDomain(null));
    assertThrows(UnsupportedOperationException.class, () -> service.DeprecateDomain(null, null));
  }

  public void testRestartWorkflowExecution() {
    assertThrows(UnsupportedOperationException.class, () -> service.RestartWorkflowExecution(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.RestartWorkflowExecution(null, null));
  }

  public void testStartWorkflowExecution() {
    assertThrows(UnsupportedOperationException.class, () -> service.StartWorkflowExecution(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.StartWorkflowExecution(null, null));
  }

  public void testStartWorkflowExecutionAsync() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.StartWorkflowExecutionAsync(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.StartWorkflowExecutionAsync(null, null));
  }

  public void testGetWorkflowExecutionHistory() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.GetWorkflowExecutionHistory(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.GetWorkflowExecutionHistory(null, null));
  }

  public void testPollForDecisionTask() {
    assertThrows(UnsupportedOperationException.class, () -> service.PollForDecisionTask(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.PollForDecisionTask(null, null));
  }

  public void testRespondDecisionTaskCompleted() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondDecisionTaskCompleted(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RespondDecisionTaskCompleted(null, null));
  }

  public void testRespondDecisionTaskFailed() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondDecisionTaskFailed(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondDecisionTaskFailed(null, null));
  }

  public void testPollForActivityTask() {
    assertThrows(UnsupportedOperationException.class, () -> service.PollForActivityTask(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.PollForActivityTask(null, null));
  }

  public void testRecordActivityTaskHeartbeat() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RecordActivityTaskHeartbeat(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.RecordActivityTaskHeartbeat(null, null));
  }

  public void testRecordActivityTaskHeartbeatByID() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RecordActivityTaskHeartbeatByID(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RecordActivityTaskHeartbeatByID(null, null));
  }

  public void testRespondActivityTaskCompleted() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondActivityTaskCompleted(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RespondActivityTaskCompleted(null, null));
  }

  public void testRespondActivityTaskCompletedByID() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondActivityTaskCompletedByID(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RespondActivityTaskCompletedByID(null, null));
  }

  public void testRespondActivityTaskFailed() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondActivityTaskFailed(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondActivityTaskFailed(null, null));
  }

  public void testRespondActivityTaskFailedByID() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondActivityTaskFailedByID(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RespondActivityTaskFailedByID(null, null));
  }

  public void testRespondActivityTaskCanceled() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondActivityTaskCanceled(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondActivityTaskCanceled(null, null));
  }

  public void testRespondActivityTaskCanceledByID() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondActivityTaskCanceledByID(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RespondActivityTaskCanceledByID(null, null));
  }

  public void testRequestCancelWorkflowExecution() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RequestCancelWorkflowExecution(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.RequestCancelWorkflowExecution(null, null));
  }

  public void testSignalWorkflowExecution() {
    assertThrows(UnsupportedOperationException.class, () -> service.SignalWorkflowExecution(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.SignalWorkflowExecution(null, null));
  }

  public void testSignalWithStartWorkflowExecution() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.SignalWithStartWorkflowExecution(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.SignalWithStartWorkflowExecution(null, null));
  }

  public void testSignalWithStartWorkflowExecutionAsync() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.SignalWithStartWorkflowExecutionAsync(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.SignalWithStartWorkflowExecutionAsync(null, null));
  }

  public void testResetWorkflowExecution() {
    assertThrows(UnsupportedOperationException.class, () -> service.ResetWorkflowExecution(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.ResetWorkflowExecution(null, null));
  }

  public void testTerminateWorkflowExecution() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.TerminateWorkflowExecution(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.TerminateWorkflowExecution(null, null));
  }

  public void testListOpenWorkflowExecutions() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.ListOpenWorkflowExecutions(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.ListOpenWorkflowExecutions(null, null));
  }

  public void testListClosedWorkflowExecutions() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.ListClosedWorkflowExecutions(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ListClosedWorkflowExecutions(null, null));
  }

  public void testListWorkflowExecutions() {
    assertThrows(UnsupportedOperationException.class, () -> service.ListWorkflowExecutions(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.ListWorkflowExecutions(null, null));
  }

  public void testListArchivedWorkflowExecutions() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.ListArchivedWorkflowExecutions(null));
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.ListArchivedWorkflowExecutions(null, null));
  }

  public void testScanWorkflowExecutions() {
    assertThrows(UnsupportedOperationException.class, () -> service.ScanWorkflowExecutions(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.ScanWorkflowExecutions(null, null));
  }

  public void testCountWorkflowExecutions() {
    assertThrows(UnsupportedOperationException.class, () -> service.CountWorkflowExecutions(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.CountWorkflowExecutions(null, null));
  }

  public void testGetSearchAttributes() {
    assertThrows(UnsupportedOperationException.class, () -> service.GetSearchAttributes());
  }

  public void testRespondQueryTaskCompleted() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondQueryTaskCompleted(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.RespondQueryTaskCompleted(null, null));
  }

  public void testResetStickyTaskList() {
    assertThrows(UnsupportedOperationException.class, () -> service.ResetStickyTaskList(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.ResetStickyTaskList(null, null));
  }

  public void testQueryWorkflow() {
    assertThrows(UnsupportedOperationException.class, () -> service.QueryWorkflow(null));
    assertThrows(UnsupportedOperationException.class, () -> service.QueryWorkflow(null, null));
  }

  public void testDescribeWorkflowExecution() {
    assertThrows(
        UnsupportedOperationException.class, () -> service.DescribeWorkflowExecution(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.DescribeWorkflowExecution(null, null));
  }

  public void testDescribeTaskList() {
    assertThrows(UnsupportedOperationException.class, () -> service.DescribeTaskList(null));
    assertThrows(UnsupportedOperationException.class, () -> service.DescribeTaskList(null, null));
  }

  public void testGetClusterInfo() {
    assertThrows(UnsupportedOperationException.class, () -> service.GetClusterInfo());
  }

  public void testGetTaskListsByDomain() {
    assertThrows(UnsupportedOperationException.class, () -> service.GetTaskListsByDomain(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.GetTaskListsByDomain(null, null));
  }

  public void testListTaskListPartitions() {
    assertThrows(UnsupportedOperationException.class, () -> service.ListTaskListPartitions(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.ListTaskListPartitions(null, null));
  }

  public void testRefreshWorkflowTasks() {
    assertThrows(UnsupportedOperationException.class, () -> service.RefreshWorkflowTasks(null));
    assertThrows(
        UnsupportedOperationException.class, () -> service.RefreshWorkflowTasks(null, null));
  }

  public void testClose() {
    assertThrows(UnsupportedOperationException.class, () -> service.close());
  }

  public void testStartWorkflowExecutionWithTimeout() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.StartWorkflowExecutionWithTimeout(null, null, null));
  }

  public void testStartWorkflowExecutionAsyncWithTimeout() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.StartWorkflowExecutionAsyncWithTimeout(null, null, null));
  }

  public void testGetWorkflowExecutionHistoryWithTimeout() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.GetWorkflowExecutionHistoryWithTimeout(null, null));
  }

  public void testSignalWorkflowExecutionWithTimeout() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> service.SignalWorkflowExecutionWithTimeout(null, null, null));
  }

  public void testIsHealthy() {
    assertThrows(UnsupportedOperationException.class, () -> service.isHealthy());
  }
}
