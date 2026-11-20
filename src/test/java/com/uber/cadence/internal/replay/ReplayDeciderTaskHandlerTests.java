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

import static com.uber.cadence.internal.common.InternalUtils.createStickyTaskList;
import static com.uber.cadence.worker.NonDeterministicWorkflowPolicy.BlockWorkflow;
import static com.uber.cadence.worker.NonDeterministicWorkflowPolicy.FailWorkflow;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.StickyExecutionAttributes;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.metrics.NoopScope;
import com.uber.cadence.internal.testservice.TestWorkflowService;
import com.uber.cadence.internal.worker.DecisionTaskHandler;
import com.uber.cadence.internal.worker.SingleWorkerOptions;
import com.uber.cadence.testUtils.HistoryUtils;
import com.uber.cadence.worker.NonDeterministicWorkflowPolicy;
import com.uber.cadence.worker.WorkflowImplementationOptions;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.StatsReporter;
import java.time.Duration;
import org.junit.Test;

public class ReplayDeciderTaskHandlerTests {

  @Test
  public void ifStickyExecutionAttributesAreNotSetThenWorkflowsAreNotCached() throws Throwable {
    // Arrange
    DeciderCache cache = new DeciderCache(10, NoopScope.getInstance());
    DecisionTaskHandler taskHandler =
        new ReplayDecisionTaskHandler(
            "domain",
            setUpMockWorkflowFactory(),
            cache,
            SingleWorkerOptions.newBuilder().build(),
            null,
            Duration.ofSeconds(5),
            new TestWorkflowService(),
            null);

    // Act
    DecisionTaskHandler.Result result =
        taskHandler.handleDecisionTask(HistoryUtils.generateDecisionTaskWithInitialHistory());

    // Assert
    assertEquals(0, cache.size());
    assertNotNull(result.getTaskCompleted());
    assertNull(result.getTaskCompleted().getStickyAttributes());
  }

  @Test
  public void ifStickyExecutionAttributesAreSetThenWorkflowsAreCached() throws Throwable {
    // Arrange
    DeciderCache cache = new DeciderCache(10, NoopScope.getInstance());
    DecisionTaskHandler taskHandler =
        new ReplayDecisionTaskHandler(
            "domain",
            setUpMockWorkflowFactory(),
            cache,
            SingleWorkerOptions.newBuilder().build(),
            "sticky",
            Duration.ofSeconds(5),
            new TestWorkflowService(),
            null);

    PollForDecisionTaskResponse decisionTask =
        HistoryUtils.generateDecisionTaskWithInitialHistory();

    // Act
    DecisionTaskHandler.Result result = taskHandler.handleDecisionTask(decisionTask);

    // Assert
    assertEquals(1, cache.size());
    assertNotNull(result.getTaskCompleted());
    StickyExecutionAttributes attributes = result.getTaskCompleted().getStickyAttributes();
    assertEquals("sticky", attributes.getWorkerTaskList().getName());
    assertEquals(5, attributes.getScheduleToStartTimeoutSeconds());
  }

  @Test
  public void ifCacheIsEvictedAndPartialHistoryIsReceivedThenTaskFailedIsReturned()
      throws Throwable {
    // Arrange
    DeciderCache cache = new DeciderCache(10, NoopScope.getInstance());
    StickyExecutionAttributes attributes = new StickyExecutionAttributes();
    attributes.setWorkerTaskList(createStickyTaskList("sticky"));
    DecisionTaskHandler taskHandler =
        new ReplayDecisionTaskHandler(
            "domain",
            setUpMockWorkflowFactory(),
            cache,
            SingleWorkerOptions.newBuilder().build(),
            "sticky",
            Duration.ofSeconds(5),
            new TestWorkflowService(),
            null);

    // Act
    DecisionTaskHandler.Result result =
        taskHandler.handleDecisionTask(HistoryUtils.generateDecisionTaskWithPartialHistory());

    // Assert
    assertEquals(0, cache.size());
    assertNull(result.getTaskCompleted());
    assertNotNull(result.getTaskFailed());
  }

  @Test
  public void versionTooNewFailsDecisionTaskAndIsReportedSeparately() throws Throwable {
    assertVersionMismatchReported(BlockWorkflow, VersionMismatchError.Type.TOO_NEW);
  }

  @Test
  public void versionTooOldFailsDecisionTaskAndIsReportedSeparately() throws Throwable {
    assertVersionMismatchReported(BlockWorkflow, VersionMismatchError.Type.TOO_OLD);
  }

  @Test
  public void versionMismatchDoesNotFailWorkflowUnderFailWorkflowPolicy() throws Throwable {
    // A version mismatch is not non-determinism, so FailWorkflow must not fail the workflow.
    assertVersionMismatchReported(FailWorkflow, VersionMismatchError.Type.TOO_NEW);
  }

  @Test
  public void otherErrorsAreStillReportedAsDecisionExecutionFailures() throws Throwable {
    // Arrange
    StatsReporter reporter = mock(StatsReporter.class);
    Scope scope =
        new RootScopeBuilder()
            .reporter(reporter)
            .reportEvery(com.uber.m3.util.Duration.ofMillis(10));
    ReplayWorkflow mockWorkflow = mock(ReplayWorkflow.class);
    ReplayWorkflowFactory mockFactory = mock(ReplayWorkflowFactory.class);
    when(mockFactory.getWorkflow(any())).thenReturn(mockWorkflow);
    when(mockWorkflow.getWorkflowImplementationOptions())
        .thenReturn(
            new WorkflowImplementationOptions.Builder()
                .setNonDeterministicWorkflowPolicy(BlockWorkflow)
                .build());
    when(mockWorkflow.eventLoop()).thenThrow(new Error("simulated"));

    DecisionTaskHandler taskHandler =
        new ReplayDecisionTaskHandler(
            "domain",
            mockFactory,
            new DeciderCache(10, NoopScope.getInstance()),
            SingleWorkerOptions.newBuilder().setMetricsScope(scope).build(),
            null,
            Duration.ofSeconds(5),
            new TestWorkflowService(),
            null);

    // Act
    DecisionTaskHandler.Result result =
        taskHandler.handleDecisionTask(HistoryUtils.generateDecisionTaskWithInitialHistory());

    // Assert
    assertNotNull(result.getTaskFailed());
    Thread.sleep(200);
    verify(reporter, times(1))
        .reportCounter(eq(MetricsType.DECISION_EXECUTION_FAILED_COUNTER), any(), eq(1L));
    verify(reporter, never())
        .reportCounter(eq(MetricsType.DECISION_TASK_VERSION_MISMATCH_COUNTER), any(), anyLong());
  }

  private void assertVersionMismatchReported(
      NonDeterministicWorkflowPolicy policy, VersionMismatchError.Type type) throws Throwable {
    // Arrange
    StatsReporter reporter = mock(StatsReporter.class);
    Scope scope =
        new RootScopeBuilder()
            .reporter(reporter)
            .reportEvery(com.uber.m3.util.Duration.ofMillis(10));
    ReplayWorkflow mockWorkflow = mock(ReplayWorkflow.class);
    ReplayWorkflowFactory mockFactory = mock(ReplayWorkflowFactory.class);
    when(mockFactory.getWorkflow(any())).thenReturn(mockWorkflow);
    when(mockWorkflow.getWorkflowImplementationOptions())
        .thenReturn(
            new WorkflowImplementationOptions.Builder()
                .setNonDeterministicWorkflowPolicy(policy)
                .build());
    when(mockWorkflow.eventLoop()).thenThrow(new VersionMismatchError(type, "change1", 2, 0, 1));

    DecisionTaskHandler taskHandler =
        new ReplayDecisionTaskHandler(
            "domain",
            mockFactory,
            new DeciderCache(10, NoopScope.getInstance()),
            SingleWorkerOptions.newBuilder().setMetricsScope(scope).build(),
            null,
            Duration.ofSeconds(5),
            new TestWorkflowService(),
            null);

    // Act
    DecisionTaskHandler.Result result =
        taskHandler.handleDecisionTask(HistoryUtils.generateDecisionTaskWithInitialHistory());

    // Assert: the decision task fails so it is retried; the workflow is not completed or failed.
    assertNull(result.getTaskCompleted());
    assertNotNull(result.getTaskFailed());

    // Wait for reporter
    Thread.sleep(200);
    verify(reporter, times(1))
        .reportCounter(
            MetricsType.DECISION_TASK_VERSION_MISMATCH_COUNTER,
            ImmutableMap.of(MetricsTag.VERSION_MISMATCH_TYPE, type.getTagValue()),
            1);
    verify(reporter, never())
        .reportCounter(eq(MetricsType.DECISION_EXECUTION_FAILED_COUNTER), any(), anyLong());
    verify(reporter, never())
        .reportCounter(eq(MetricsType.DECISION_TASK_ERROR_COUNTER), any(), anyLong());
  }

  private ReplayWorkflowFactory setUpMockWorkflowFactory() throws Throwable {
    ReplayWorkflow mockWorkflow = mock(ReplayWorkflow.class);
    ReplayWorkflowFactory mockFactory = mock(ReplayWorkflowFactory.class);

    when(mockFactory.getWorkflow(any())).thenReturn(mockWorkflow);
    when(mockWorkflow.eventLoop()).thenReturn(true);
    return mockFactory;
  }
}
