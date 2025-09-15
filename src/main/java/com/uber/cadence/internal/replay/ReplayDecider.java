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

import static com.uber.cadence.worker.NonDeterministicWorkflowPolicy.FailWorkflow;

import com.google.common.annotations.VisibleForTesting;
import com.uber.cadence.*;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.internal.common.OptionsUtils;
import com.uber.cadence.internal.common.RpcRetryer;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.replay.HistoryHelper.DecisionEvents;
import com.uber.cadence.internal.replay.HistoryHelper.DecisionEventsIterator;
import com.uber.cadence.internal.worker.DecisionTaskWithHistoryIterator;
import com.uber.cadence.internal.worker.LocalActivityWorker;
import com.uber.cadence.internal.worker.SingleWorkerOptions;
import com.uber.cadence.internal.worker.WorkflowExecutionException;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.workflow.Functions;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.Stopwatch;
import com.uber.m3.util.ImmutableMap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements decider that relies on replay of a workflow code. An instance of this class is created
 * per decision.
 */
class ReplayDecider implements Decider {

  private static final Logger log = LoggerFactory.getLogger(ReplayDecider.class);

  private static final int MAXIMUM_PAGE_SIZE = 10000;

  private final DecisionsHelper decisionsHelper;
  private final DecisionContextImpl context;
  private final IWorkflowService service;
  private final ReplayWorkflow workflow;
  private boolean cancelRequested;
  private boolean completed;
  private WorkflowExecutionException failure;
  private long wakeUpTime;
  private Consumer<Exception> timerCancellationHandler;
  private final Scope metricsScope;
  private final long wfStartTimeNanos;
  private final WorkflowExecutionStartedEventAttributes startedEvent;
  private final Lock lock = new ReentrantLock();
  private final Consumer<HistoryEvent> localActivityCompletionSink;

  ReplayDecider(
      IWorkflowService service,
      String domain,
      WorkflowType workflowType,
      ReplayWorkflow workflow,
      DecisionsHelper decisionsHelper,
      SingleWorkerOptions options,
      BiFunction<LocalActivityWorker.Task, Duration, Boolean> laTaskPoller) {
    this.service = service;
    this.workflow = workflow;
    this.decisionsHelper = decisionsHelper;
    this.metricsScope =
        options
            .getMetricsScope()
            .tagged(ImmutableMap.of(MetricsTag.WORKFLOW_TYPE, workflowType.getName()));
    PollForDecisionTaskResponse decisionTask = decisionsHelper.getTask();

    startedEvent =
        decisionTask.getHistory().getEvents().get(0).getWorkflowExecutionStartedEventAttributes();
    if (startedEvent == null) {
      throw new IllegalArgumentException(
          "First event in the history is not WorkflowExecutionStarted");
    }
    wfStartTimeNanos = decisionTask.getHistory().getEvents().get(0).getTimestamp();

    context =
        new DecisionContextImpl(
            decisionsHelper, domain, decisionTask, startedEvent, options, laTaskPoller, this);
    localActivityCompletionSink =
        historyEvent -> {
          lock.lock();
          try {
            processEvent(historyEvent);
          } finally {
            lock.unlock();
          }
        };
  }

  Lock getLock() {
    return lock;
  }

  private void handleWorkflowExecutionStarted(HistoryEvent event) {
    workflow.start(event, context);
  }

  private void processEvent(HistoryEvent event) {
    EventType eventType = event.getEventType();
    switch (eventType) {
      case ActivityTaskCanceled:
        context.handleActivityTaskCanceled(event);
        break;
      case ActivityTaskCompleted:
        context.handleActivityTaskCompleted(event);
        break;
      case ActivityTaskFailed:
        context.handleActivityTaskFailed(event);
        break;
      case ActivityTaskStarted:
        decisionsHelper.handleActivityTaskStarted(event);
        break;
      case ActivityTaskTimedOut:
        context.handleActivityTaskTimedOut(event);
        break;
      case ExternalWorkflowExecutionCancelRequested:
        context.handleChildWorkflowExecutionCancelRequested(event);
        decisionsHelper.handleExternalWorkflowExecutionCancelRequested(event);
        break;
      case ChildWorkflowExecutionCanceled:
        context.handleChildWorkflowExecutionCanceled(event);
        break;
      case ChildWorkflowExecutionCompleted:
        context.handleChildWorkflowExecutionCompleted(event);
        break;
      case ChildWorkflowExecutionFailed:
        context.handleChildWorkflowExecutionFailed(event);
        break;
      case ChildWorkflowExecutionStarted:
        context.handleChildWorkflowExecutionStarted(event);
        break;
      case ChildWorkflowExecutionTerminated:
        context.handleChildWorkflowExecutionTerminated(event);
        break;
      case ChildWorkflowExecutionTimedOut:
        context.handleChildWorkflowExecutionTimedOut(event);
        break;
      case DecisionTaskCompleted:
      case DecisionTaskScheduled:
      case WorkflowExecutionTimedOut:
      case WorkflowExecutionTerminated:
        // NOOP
        break;
      case DecisionTaskStarted:
        throw new IllegalArgumentException("not expected");
      case DecisionTaskTimedOut:
        // Handled in the processEvent(event)
        break;
      case ExternalWorkflowExecutionSignaled:
        context.handleExternalWorkflowExecutionSignaled(event);
        break;
      case StartChildWorkflowExecutionFailed:
        context.handleStartChildWorkflowExecutionFailed(event);
        break;
      case TimerFired:
        handleTimerFired(event);
        break;
      case WorkflowExecutionCancelRequested:
        handleWorkflowExecutionCancelRequested(event);
        break;
      case WorkflowExecutionSignaled:
        handleWorkflowExecutionSignaled(event);
        break;
      case WorkflowExecutionStarted:
        handleWorkflowExecutionStarted(event);
        break;
      case ActivityTaskScheduled:
        decisionsHelper.handleActivityTaskScheduled(event);
        break;
      case ActivityTaskCancelRequested:
        decisionsHelper.handleActivityTaskCancelRequested(event);
        break;
      case RequestCancelActivityTaskFailed:
        decisionsHelper.handleRequestCancelActivityTaskFailed(event);
        break;
      case MarkerRecorded:
        context.handleMarkerRecorded(event);
        break;
      case WorkflowExecutionCompleted:
      case WorkflowExecutionFailed:
      case WorkflowExecutionCanceled:
      case WorkflowExecutionContinuedAsNew:
        break;
      case TimerStarted:
        decisionsHelper.handleTimerStarted(event);
        break;
      case TimerCanceled:
        context.handleTimerCanceled(event);
        break;
      case SignalExternalWorkflowExecutionInitiated:
        decisionsHelper.handleSignalExternalWorkflowExecutionInitiated(event);
        break;
      case SignalExternalWorkflowExecutionFailed:
        context.handleSignalExternalWorkflowExecutionFailed(event);
        break;
      case RequestCancelExternalWorkflowExecutionInitiated:
        decisionsHelper.handleRequestCancelExternalWorkflowExecutionInitiated(event);
        break;
      case RequestCancelExternalWorkflowExecutionFailed:
        decisionsHelper.handleRequestCancelExternalWorkflowExecutionFailed(event);
        break;
      case StartChildWorkflowExecutionInitiated:
        decisionsHelper.handleStartChildWorkflowExecutionInitiated(event);
        break;
      case CancelTimerFailed:
        decisionsHelper.handleCancelTimerFailed(event);
        break;
      case DecisionTaskFailed:
        context.handleDecisionTaskFailed(event);
        break;
      case UpsertWorkflowSearchAttributes:
        context.handleUpsertSearchAttributes(event);
        break;
    }
  }

  private void eventLoop() {
    if (completed) {
      return;
    }
    try {
      completed = workflow.eventLoop();
    } catch (Error e) {
      throw e;
    } catch (WorkflowExecutionException e) {
      failure = e;
      completed = true;
    } catch (CancellationException e) {
      if (!cancelRequested) {
        failure = workflow.mapUnexpectedException(e);
      }
      completed = true;
    } catch (Throwable e) {
      // can cast as Error is caught above.
      failure = workflow.mapUnexpectedException((Exception) e);
      completed = true;
    }
  }

  private void mayBeCompleteWorkflow() {
    if (completed) {
      completeWorkflow();
    } else {
      updateTimers();
    }
  }

  private void completeWorkflow() {
    if (failure != null) {
      decisionsHelper.failWorkflowExecution(failure);
      metricsScope.counter(MetricsType.WORKFLOW_FAILED_COUNTER).inc(1);
    } else if (cancelRequested) {
      decisionsHelper.cancelWorkflowExecution();
      metricsScope.counter(MetricsType.WORKFLOW_CANCELLED_COUNTER).inc(1);
    } else {
      ContinueAsNewWorkflowExecutionParameters continueAsNewOnCompletion =
          context.getContinueAsNewOnCompletion();
      if (continueAsNewOnCompletion != null) {
        decisionsHelper.continueAsNewWorkflowExecution(continueAsNewOnCompletion);
        metricsScope.counter(MetricsType.WORKFLOW_CONTINUE_AS_NEW_COUNTER).inc(1);
      } else {
        byte[] workflowOutput = workflow.getOutput();
        decisionsHelper.completeWorkflowExecution(workflowOutput);
        metricsScope.counter(MetricsType.WORKFLOW_COMPLETED_COUNTER).inc(1);
      }
    }

    long nanoTime = TimeUnit.NANOSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    com.uber.m3.util.Duration d = com.uber.m3.util.Duration.ofNanos(nanoTime - wfStartTimeNanos);
    metricsScope.timer(MetricsType.WORKFLOW_E2E_LATENCY).record(d);
  }

  private void updateTimers() {
    long nextWakeUpTime = workflow.getNextWakeUpTime();
    if (nextWakeUpTime == 0) {
      if (timerCancellationHandler != null) {
        timerCancellationHandler.accept(null);
        timerCancellationHandler = null;
      }
      wakeUpTime = nextWakeUpTime;
      return;
    }
    if (wakeUpTime == nextWakeUpTime && timerCancellationHandler != null) {
      return; // existing timer
    }
    long delayMilliseconds = nextWakeUpTime - context.currentTimeMillis();
    if (delayMilliseconds < 0) {
      throw new IllegalStateException("Negative delayMilliseconds=" + delayMilliseconds);
    }
    // Round up to the nearest second as we don't want to deliver a timer
    // earlier than requested.
    long delaySeconds =
        OptionsUtils.roundUpToSeconds(Duration.ofMillis(delayMilliseconds)).getSeconds();
    if (timerCancellationHandler != null) {
      timerCancellationHandler.accept(null);
      timerCancellationHandler = null;
    }
    wakeUpTime = nextWakeUpTime;
    timerCancellationHandler =
        context.createTimer(
            delaySeconds,
            (t) -> {
              // Intentionally left empty.
              // Timer ensures that decision is scheduled at the time workflow can make progress.
              // But no specific timer related action is necessary as Workflow.sleep is just a
              // Workflow.await with a time based condition.
            });
  }

  private void handleWorkflowExecutionCancelRequested(HistoryEvent event) {
    context.setCancelRequested(true);
    String cause = event.getWorkflowExecutionCancelRequestedEventAttributes().getCause();
    workflow.cancel(cause);
    cancelRequested = true;
  }

  private void handleTimerFired(HistoryEvent event) {
    TimerFiredEventAttributes attributes = event.getTimerFiredEventAttributes();
    String timerId = attributes.getTimerId();
    if (timerId.equals(DecisionsHelper.FORCE_IMMEDIATE_DECISION_TIMER)) {
      return;
    }
    context.handleTimerFired(attributes);
  }

  private void handleWorkflowExecutionSignaled(HistoryEvent event) {
    assert (event.getEventType() == EventType.WorkflowExecutionSignaled);
    final WorkflowExecutionSignaledEventAttributes signalAttributes =
        event.getWorkflowExecutionSignaledEventAttributes();
    if (completed) {
      throw new IllegalStateException("Signal received after workflow is closed.");
    }
    this.workflow.handleSignal(
        signalAttributes.getSignalName(), signalAttributes.getInput(), event.getEventId());
  }

  @Override
  public DecisionResult decide(PollForDecisionTaskResponse decisionTask) throws Throwable {
    lock.lock();
    try {
      AtomicReference<Map<String, WorkflowQueryResult>> queryResults = new AtomicReference<>();
      boolean forceCreateNewDecisionTask =
          decideImpl(
              decisionTask, () -> queryResults.set(getQueryResults(decisionTask.getQueries())));
      return new DecisionResult(
          decisionsHelper.getDecisions(), queryResults.get(), forceCreateNewDecisionTask);
    } finally {
      lock.unlock();
    }
  }

  private Map<String, WorkflowQueryResult> getQueryResults(Map<String, WorkflowQuery> queries) {
    if (queries == null) {
      return null;
    }

    return queries
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, q -> queryWorkflow(q.getValue())));
  }

  private WorkflowQueryResult queryWorkflow(WorkflowQuery query) {
    try {
      return new WorkflowQueryResult()
          .setResultType(QueryResultType.ANSWERED)
          .setAnswer(workflow.query(query));
    } catch (Throwable e) {
      return new WorkflowQueryResult()
          .setResultType(QueryResultType.FAILED)
          .setErrorMessage(e.getMessage());
    }
  }

  // Returns boolean to indicate whether we need to force create new decision task for local
  // activity heartbeating.
  private boolean decideImpl(PollForDecisionTaskResponse decisionTask, Functions.Proc query)
      throws Throwable {
    boolean forceCreateNewDecisionTask = false;
    try {
      long startTime = System.currentTimeMillis();
      DecisionTaskWithHistoryIterator decisionTaskWithHistoryIterator =
          new DecisionTaskWithHistoryIteratorImpl(
              decisionTask, Duration.ofSeconds(startedEvent.getTaskStartToCloseTimeoutSeconds()));
      HistoryHelper historyHelper =
          new HistoryHelper(
              decisionTaskWithHistoryIterator, context.getReplayCurrentTimeMilliseconds());
      DecisionEventsIterator iterator = historyHelper.getIterator();
      if ((decisionsHelper.getNextDecisionEventId()
              != historyHelper.getPreviousStartedEventId()
                  + 2) // getNextDecisionEventId() skips over completed.
          && (decisionsHelper.getNextDecisionEventId() != 0
              && historyHelper.getPreviousStartedEventId() != 0)
          && (decisionTask.getHistory().getEvents() != null
              && decisionTask.getHistory().getEvents().size() > 0)) {
        throw new IllegalStateException(
            String.format(
                "ReplayDecider expects next event id at %d. History's previous started event id is %d",
                decisionsHelper.getNextDecisionEventId(),
                historyHelper.getPreviousStartedEventId()));
      }

      while (iterator.hasNext()) {
        DecisionEvents decision = iterator.next();
        context.setReplaying(decision.isReplay());
        context.setReplayCurrentTimeMilliseconds(decision.getReplayCurrentTimeMilliseconds());

        decisionsHelper.handleDecisionTaskStartedEvent(decision);
        // Markers must be cached first as their data is needed when processing events.
        for (HistoryEvent event : decision.getMarkers()) {
          if (!event
              .getMarkerRecordedEventAttributes()
              .getMarkerName()
              .equals(ClockDecisionContext.LOCAL_ACTIVITY_MARKER_NAME)) {
            processEvent(event);
          }
        }

        for (HistoryEvent event : decision.getEvents()) {
          processEvent(event);
        }

        forceCreateNewDecisionTask =
            processEventLoop(
                startTime,
                startedEvent.getTaskStartToCloseTimeoutSeconds(),
                decision,
                decisionTask.getQuery() != null);

        mayBeCompleteWorkflow();
        if (decision.isReplay()) {
          decisionsHelper.notifyDecisionSent();
        }
        // Updates state machines with results of the previous decisions
        for (HistoryEvent event : decision.getDecisionEvents()) {
          processEvent(event);
        }
        // Reset state to before running the event loop
        decisionsHelper.handleDecisionTaskStartedEvent(decision);
      }
      if (forceCreateNewDecisionTask) {
        metricsScope.counter(MetricsType.DECISION_TASK_FORCE_COMPLETED).inc(1);
      }
      return forceCreateNewDecisionTask;
    } catch (Error e) {
      if (this.workflow.getWorkflowImplementationOptions().getNonDeterministicWorkflowPolicy()
          == FailWorkflow) {
        // fail workflow
        failure = workflow.mapError(e);
        completed = true;
        completeWorkflow();
        return false;
      } else {
        metricsScope.counter(MetricsType.DECISION_TASK_ERROR_COUNTER).inc(1);
        // fail decision, not a workflow
        throw e;
      }
    } finally {
      if (query != null) {
        query.apply();
      }
      if (completed) {
        close();
      }
    }
  }

  private boolean processEventLoop(
      long startTime, int decisionTimeoutSecs, DecisionEvents decision, boolean isQuery)
      throws Throwable {
    eventLoop();

    if (decision.isReplay() || isQuery) {
      return replayLocalActivities(decision);
    } else {
      return executeLocalActivities(startTime, decisionTimeoutSecs);
    }
  }

  private boolean replayLocalActivities(DecisionEvents decision) throws Throwable {
    List<HistoryEvent> localActivityMarkers = new ArrayList<>();
    for (HistoryEvent event : decision.getMarkers()) {
      if (event
          .getMarkerRecordedEventAttributes()
          .getMarkerName()
          .equals(ClockDecisionContext.LOCAL_ACTIVITY_MARKER_NAME)) {
        localActivityMarkers.add(event);
      }
    }

    if (localActivityMarkers.isEmpty()) {
      return false;
    }

    int processed = 0;
    while (context.numPendingLaTasks() > 0) {
      int numTasks = context.numPendingLaTasks();
      for (HistoryEvent event : localActivityMarkers) {
        processEvent(event);
      }

      eventLoop();

      processed += numTasks;
      if (processed == localActivityMarkers.size()) {
        return false;
      }
    }
    return false;
  }

  // Return whether we would need a new decision task immediately.
  private boolean executeLocalActivities(long startTime, int decisionTimeoutSecs) {
    Duration maxProcessingTime = Duration.ofSeconds((long) (0.8 * decisionTimeoutSecs));

    while (context.numPendingLaTasks() > 0) {
      Duration processingTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
      Duration maxWaitAllowed = maxProcessingTime.minus(processingTime);

      boolean started = context.startUnstartedLaTasks(maxWaitAllowed);
      if (!started) {
        // We were not able to send the current batch of la tasks before deadline.
        // Return true to indicate that we need a new decision task immediately.
        return true;
      }

      try {
        context.awaitTaskCompletion(maxWaitAllowed);
      } catch (InterruptedException e) {
        return true;
      }

      eventLoop();

      if (context.numPendingLaTasks() == 0) {
        return false;
      }

      // Break local activity processing loop if we almost reach decision task timeout.
      processingTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
      if (processingTime.compareTo(maxProcessingTime) > 0) {
        return true;
      }
    }
    return false;
  }

  int getDecisionTimeoutSeconds() {
    return startedEvent.getTaskStartToCloseTimeoutSeconds();
  }

  @Override
  public void close() {
    lock.lock();
    try {
      workflow.close();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public byte[] query(PollForDecisionTaskResponse response, WorkflowQuery query) throws Throwable {
    lock.lock();
    try {
      AtomicReference<byte[]> result = new AtomicReference<>();
      decideImpl(response, () -> result.set(workflow.query(query)));
      return result.get();
    } finally {
      lock.unlock();
    }
  }

  public Consumer<HistoryEvent> getLocalActivityCompletionSink() {
    return localActivityCompletionSink;
  }

  private class DecisionTaskWithHistoryIteratorImpl implements DecisionTaskWithHistoryIterator {

    private final Duration retryServiceOperationInitialInterval = Duration.ofMillis(200);
    private final Duration retryServiceOperationMaxInterval = Duration.ofSeconds(4);
    private final Duration paginationStart = Duration.ofMillis(System.currentTimeMillis());
    private final Duration decisionTaskStartToCloseTimeout;

    private Duration decisionTaskRemainingTime() {
      Duration passed = Duration.ofMillis(System.currentTimeMillis()).minus(paginationStart);
      return decisionTaskStartToCloseTimeout.minus(passed);
    }

    private final PollForDecisionTaskResponse task;
    private Iterator<HistoryEvent> current;
    private byte[] nextPageToken;

    @VisibleForTesting
    DecisionTaskWithHistoryIteratorImpl(
        PollForDecisionTaskResponse task, Duration decisionTaskStartToCloseTimeout) {
      this.task = Objects.requireNonNull(task);
      this.decisionTaskStartToCloseTimeout =
          Objects.requireNonNull(decisionTaskStartToCloseTimeout);

      History history = task.getHistory();
      current = history.getEvents().iterator();
      nextPageToken = task.getNextPageToken();
    }

    @Override
    public PollForDecisionTaskResponse getDecisionTask() {
      lock.lock();
      try {
        return task;
      } finally {
        lock.unlock();
      }
    }

    @Override
    public Iterator<HistoryEvent> getHistory() {
      return new Iterator<HistoryEvent>() {
        @Override
        public boolean hasNext() {
          return current.hasNext() || nextPageToken != null;
        }

        @Override
        public HistoryEvent next() {
          if (current.hasNext()) {
            return current.next();
          }

          Duration decisionTaskRemainingTime = decisionTaskRemainingTime();
          if (decisionTaskRemainingTime.isNegative() || decisionTaskRemainingTime.isZero()) {
            throw new Error(
                "Decision task timed out while querying history. If this happens consistently please consider "
                    + "increase decision task timeout or reduce history size.");
          }

          metricsScope.counter(MetricsType.WORKFLOW_GET_HISTORY_COUNTER).inc(1);
          Stopwatch sw = metricsScope.timer(MetricsType.WORKFLOW_GET_HISTORY_LATENCY).start();
          RetryOptions retryOptions =
              new RetryOptions.Builder()
                  .setExpiration(decisionTaskRemainingTime)
                  .setInitialInterval(retryServiceOperationInitialInterval)
                  .setMaximumInterval(retryServiceOperationMaxInterval)
                  .validateBuildWithDefaults();

          GetWorkflowExecutionHistoryRequest request = new GetWorkflowExecutionHistoryRequest();
          request
              .setDomain(context.getDomain())
              .setExecution(task.getWorkflowExecution())
              .setMaximumPageSize(MAXIMUM_PAGE_SIZE)
              .setNextPageToken(nextPageToken);

          try {
            GetWorkflowExecutionHistoryResponse r =
                RpcRetryer.retryWithResult(
                    retryOptions, () -> service.GetWorkflowExecutionHistory(request));
            current = r.getHistory().getEvents().iterator();
            nextPageToken = r.getNextPageToken();
            metricsScope.counter(MetricsType.WORKFLOW_GET_HISTORY_SUCCEED_COUNTER).inc(1);
            sw.stop();
          } catch (BaseError e) {
            metricsScope.counter(MetricsType.WORKFLOW_GET_HISTORY_FAILED_COUNTER).inc(1);
            throw new Error(e);
          }
          if (!current.hasNext()) {
            log.error(
                "GetWorkflowExecutionHistory returns an empty history, maybe a bug in server, workflowID:{}, runID:{}, domain:{} token:{}",
                request.getExecution().getWorkflowId(),
                request.getExecution().getRunId(),
                request.getDomain(),
                Arrays.toString(request.getNextPageToken()));
            throw new Error(
                "GetWorkflowExecutionHistory return empty history, maybe a bug in server");
          }
          return current.next();
        }
      };
    }
  }
}
