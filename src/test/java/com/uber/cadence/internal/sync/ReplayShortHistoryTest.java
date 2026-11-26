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

package com.uber.cadence.internal.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uber.cadence.*;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.activity.LocalActivityOptions;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.metrics.NoopScope;
import com.uber.cadence.internal.replay.DeciderCache;
import com.uber.cadence.internal.replay.ReplayDecisionTaskHandler;
import com.uber.cadence.internal.worker.DecisionTaskHandler;
import com.uber.cadence.internal.worker.LocalActivityWorker;
import com.uber.cadence.internal.worker.SingleWorkerOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.worker.WorkflowImplementationOptions;
import com.uber.cadence.workflow.QueryMethod;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.StatsReporter;
import io.opentracing.noop.NoopTracerFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.Test;

/** A decision task whose history ends before its startedEventId must fail, not complete. */
public class ReplayShortHistoryTest {

  public interface Wf {
    @WorkflowMethod
    void run();

    @QueryMethod
    String state();
  }

  public static class WfImpl implements Wf {
    @Override
    public void run() {
      Workflow.newUntypedActivityStub(
              new ActivityOptions.Builder()
                  .setScheduleToCloseTimeout(Duration.ofMinutes(1))
                  .build())
          .execute("a", Void.class);
    }

    @Override
    public String state() {
      return "ok";
    }
  }

  public interface WfLocal {
    @WorkflowMethod
    void run();
  }

  public static class WfLocalImpl implements WfLocal {
    @Override
    public void run() {
      Workflow.newUntypedLocalActivityStub(
              new LocalActivityOptions.Builder()
                  .setScheduleToCloseTimeout(Duration.ofMinutes(1))
                  .build())
          .execute("la", Void.class);
    }
  }

  @Test
  public void historyEndingBeforeStartedEventIdFailsTheTask() throws Exception {
    // The task started at event 3, but a stale read stopped one event short, at the
    // DecisionTaskScheduled.
    StatsReporter reporter = mock(StatsReporter.class);
    Scope scope =
        new RootScopeBuilder()
            .reporter(reporter)
            .reportEvery(com.uber.m3.util.Duration.ofSeconds(3600));

    assertPrematureEnd(
        newHandler(null, mock(IWorkflowService.class), scope)
            .handleDecisionTask(
                task(workflowStarted("Wf::run"), decisionScheduled()).setStartedEventId(3)));
    scope.close();
    verify(reporter, times(1))
        .reportCounter(eq(MetricsType.DECISION_TASK_SHORT_HISTORY_COUNTER), anyMap(), eq(1L));
  }

  @Test
  public void emptyHistoryFailsTheTask() throws Exception {
    // A sticky task for a cached decider, with no new events.
    DecisionTaskHandler handler =
        newHandler(null, mock(IWorkflowService.class), NoopScope.getInstance());
    handler.handleDecisionTask(
        task(workflowStarted("Wf::run"), decisionScheduled(), decisionStarted())
            .setStartedEventId(3));

    assertPrematureEnd(
        handler.handleDecisionTask(
            task(workflowStarted("Wf::run"))
                .setHistory(new History().setEvents(Collections.emptyList()))
                .setPreviousStartedEventId(3)
                .setStartedEventId(6)));
  }

  @Test
  public void historyWithMorePagesCompletes() throws Exception {
    IWorkflowService service = mock(IWorkflowService.class);
    when(service.GetWorkflowExecutionHistory(any()))
        .thenReturn(
            new GetWorkflowExecutionHistoryResponse()
                .setHistory(new History().setEvents(Collections.singletonList(decisionStarted()))));
    PollForDecisionTaskResponse task =
        task(workflowStarted("Wf::run"), decisionScheduled())
            .setNextPageToken(new byte[] {1})
            .setStartedEventId(3);

    assertNotNull(
        newHandler(null, service, NoopScope.getInstance())
            .handleDecisionTask(task)
            .getTaskCompleted());
  }

  @Test
  public void shortHistoryDoesNotRunLocalActivities() throws Exception {
    // Processing the decision task started at event 3 would start the local activity.
    AtomicInteger localActivities = new AtomicInteger();
    DecisionTaskHandler handler =
        newHandler(
            (task, timeout) -> {
              localActivities.incrementAndGet();
              throw new IllegalStateException("local activity started");
            },
            mock(IWorkflowService.class),
            NoopScope.getInstance());

    assertPrematureEnd(
        handler.handleDecisionTask(
            task(workflowStarted("WfLocal::run"), decisionScheduled(), decisionStarted())
                .setStartedEventId(9)));
    assertEquals(0, localActivities.get());
  }

  @Test
  public void localQueryIsNotChecked() throws Exception {
    // Local replay and query (WorkflowWorker.queryWorkflowExecution) use Long.MAX_VALUE.
    PollForDecisionTaskResponse task =
        task(workflowStarted("Wf::run"), decisionScheduled(), decisionStarted())
            .setPreviousStartedEventId(Long.MAX_VALUE)
            .setStartedEventId(Long.MAX_VALUE)
            .setQuery(new WorkflowQuery().setQueryType("Wf::state"));

    DecisionTaskHandler.Result result =
        newHandler(null, mock(IWorkflowService.class), NoopScope.getInstance())
            .handleDecisionTask(task);
    assertEquals(
        result.getQueryCompleted().getErrorMessage(),
        QueryTaskCompletedType.COMPLETED,
        result.getQueryCompleted().getCompletedType());
  }

  private static void assertPrematureEnd(DecisionTaskHandler.Result result) {
    assertNull("task completed on a short history", result.getTaskCompleted());
    String details = new String(result.getTaskFailed().getDetails(), StandardCharsets.UTF_8);
    assertTrue(details, details.contains("Premature end of history"));
  }

  private static DecisionTaskHandler newHandler(
      BiFunction<LocalActivityWorker.Task, Duration, Boolean> localActivityPoller,
      IWorkflowService service,
      Scope metricsScope) {
    DeciderCache cache = new DeciderCache(10, NoopScope.getInstance());
    POJOWorkflowImplementationFactory factory =
        new POJOWorkflowImplementationFactory(
            JsonDataConverter.getInstance(),
            Executors.newCachedThreadPool(),
            i -> i,
            cache,
            Collections.emptyList(),
            NoopTracerFactory.create());
    factory.setRegistry(
        RegistryInternal.EMPTY.withWorkflowImplementationTypes(
            new WorkflowImplementationOptions.Builder().build(), WfImpl.class, WfLocalImpl.class));
    return new ReplayDecisionTaskHandler(
        "domain",
        factory,
        cache,
        SingleWorkerOptions.newBuilder().setMetricsScope(metricsScope).build(),
        "sticky",
        Duration.ofSeconds(5),
        service,
        localActivityPoller);
  }

  private static PollForDecisionTaskResponse task(HistoryEvent... events) {
    return new PollForDecisionTaskResponse()
        .setTaskToken(new byte[] {1})
        .setWorkflowExecution(new WorkflowExecution().setWorkflowId("w").setRunId("r"))
        .setWorkflowType(events[0].getWorkflowExecutionStartedEventAttributes().getWorkflowType())
        .setHistory(new History().setEvents(Arrays.asList(events)));
  }

  private static HistoryEvent workflowStarted(String workflowType) {
    return event(1, EventType.WorkflowExecutionStarted)
        .setWorkflowExecutionStartedEventAttributes(
            new WorkflowExecutionStartedEventAttributes()
                .setWorkflowType(new WorkflowType().setName(workflowType))
                .setTaskList(new TaskList().setName("tl"))
                .setExecutionStartToCloseTimeoutSeconds(100)
                .setTaskStartToCloseTimeoutSeconds(10));
  }

  private static HistoryEvent decisionScheduled() {
    return event(2, EventType.DecisionTaskScheduled)
        .setDecisionTaskScheduledEventAttributes(new DecisionTaskScheduledEventAttributes());
  }

  private static HistoryEvent decisionStarted() {
    return event(3, EventType.DecisionTaskStarted)
        .setDecisionTaskStartedEventAttributes(
            new DecisionTaskStartedEventAttributes().setScheduledEventId(2));
  }

  private static HistoryEvent event(long id, EventType type) {
    return new HistoryEvent().setEventId(id).setEventType(type).setTimestamp(id * 1_000_000_000L);
  }
}
