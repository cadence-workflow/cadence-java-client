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

import com.uber.cadence.*;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.internal.replay.MarkerHandler.MarkerData;
import com.uber.cadence.internal.sync.WorkflowInternal;
import com.uber.cadence.internal.worker.LocalActivityPollTask;
import com.uber.cadence.internal.worker.LocalActivityWorker;
import com.uber.cadence.workflow.ActivityFailureException;
import com.uber.cadence.workflow.Functions.Func;
import com.uber.cadence.workflow.Functions.Func1;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Clock that must be used inside workflow definition code to ensure replay determinism. */
public final class ClockDecisionContext {

  private static final String SIDE_EFFECT_MARKER_NAME = "SideEffect";
  private static final String MUTABLE_SIDE_EFFECT_MARKER_NAME = "MutableSideEffect";
  public static final String VERSION_MARKER_NAME = "Version";
  public static final String LOCAL_ACTIVITY_MARKER_NAME = "LocalActivity";

  private static final Logger log = LoggerFactory.getLogger(ClockDecisionContext.class);

  private final class TimerCancellationHandler implements Consumer<Exception> {

    private final long startEventId;

    TimerCancellationHandler(long timerId) {
      this.startEventId = timerId;
    }

    @Override
    public void accept(Exception reason) {
      decisions.cancelTimer(startEventId, () -> timerCancelled(startEventId, reason));
    }
  }

  private final DecisionsHelper decisions;
  private final Map<Long, OpenRequestInfo<?, Long>> scheduledTimers =
      new HashMap<>(); // key is startedEventId
  private long replayCurrentTimeMilliseconds = -1;
  private boolean replaying = true;
  private final Map<Long, byte[]> sideEffectResults =
      new HashMap<>(); // Key is side effect marker eventId
  private final MarkerHandler mutableSideEffectHandler;
  private final MarkerHandler versionHandler;
  private final LocalActivityPollTask laPollTask;
  private final Map<String, OpenRequestInfo<byte[], ActivityType>> pendingLaTasks = new HashMap<>();
  private final List<ExecuteActivityParameters> unstartedLaTasks = new LinkedList<>();
  private ReplayDecider replayDecider;

  ClockDecisionContext(DecisionsHelper decisions, LocalActivityPollTask laPollTask) {
    this.decisions = decisions;
    mutableSideEffectHandler =
        new MarkerHandler(decisions, MUTABLE_SIDE_EFFECT_MARKER_NAME, () -> replaying);
    versionHandler = new MarkerHandler(decisions, VERSION_MARKER_NAME, () -> replaying);
    this.laPollTask = laPollTask;
  }

  long currentTimeMillis() {
    return replayCurrentTimeMilliseconds;
  }

  void setReplayCurrentTimeMilliseconds(long replayCurrentTimeMilliseconds) {
    this.replayCurrentTimeMilliseconds = replayCurrentTimeMilliseconds;
  }

  void setReplayDecider(ReplayDecider replayDecider) {
    this.replayDecider = replayDecider;
  }

  boolean isReplaying() {
    return replaying;
  }

  Consumer<Exception> createTimer(long delaySeconds, Consumer<Exception> callback) {
    if (delaySeconds < 0) {
      throw new IllegalArgumentException("Negative delaySeconds: " + delaySeconds);
    }
    if (delaySeconds == 0) {
      callback.accept(null);
      return null;
    }
    long firingTime = currentTimeMillis() + TimeUnit.SECONDS.toMillis(delaySeconds);
    final OpenRequestInfo<?, Long> context = new OpenRequestInfo<>(firingTime);
    final StartTimerDecisionAttributes timer = new StartTimerDecisionAttributes();
    timer.setStartToFireTimeoutSeconds(delaySeconds);
    timer.setTimerId(String.valueOf(decisions.getAndIncrementNextId()));
    long startEventId = decisions.startTimer(timer);
    context.setCompletionHandle((ctx, e) -> callback.accept(e));
    scheduledTimers.put(startEventId, context);
    return new TimerCancellationHandler(startEventId);
  }

  void setReplaying(boolean replaying) {
    this.replaying = replaying;
  }

  void handleTimerFired(TimerFiredEventAttributes attributes) {
    long startedEventId = attributes.getStartedEventId();
    if (decisions.handleTimerClosed(attributes)) {
      OpenRequestInfo<?, Long> scheduled = scheduledTimers.remove(startedEventId);
      if (scheduled != null) {
        BiConsumer<?, Exception> completionCallback = scheduled.getCompletionCallback();
        completionCallback.accept(null, null);
      }
    }
  }

  void handleTimerCanceled(HistoryEvent event) {
    TimerCanceledEventAttributes attributes = event.getTimerCanceledEventAttributes();
    long startedEventId = attributes.getStartedEventId();
    if (decisions.handleTimerCanceled(event)) {
      timerCancelled(startedEventId, null);
    }
  }

  private void timerCancelled(long startEventId, Exception reason) {
    OpenRequestInfo<?, ?> scheduled = scheduledTimers.remove(startEventId);
    if (scheduled == null) {
      return;
    }
    BiConsumer<?, Exception> context = scheduled.getCompletionCallback();
    CancellationException exception = new CancellationException("Cancelled by request");
    exception.initCause(reason);
    context.accept(null, exception);
  }

  byte[] sideEffect(Func<byte[]> func) {
    decisions.addAllMissingVersionMarker(false, Optional.empty());
    long sideEffectEventId = decisions.getNextDecisionEventId();
    byte[] result;
    if (replaying) {
      result = sideEffectResults.get(sideEffectEventId);
      if (result == null) {
        throw new Error("No cached result found for SideEffect EventID=" + sideEffectEventId);
      }
    } else {
      try {
        result = func.apply();
      } catch (Error e) {
        throw e;
      } catch (Exception e) {
        throw new Error("sideEffect function failed", e);
      }
    }
    decisions.recordMarker(SIDE_EFFECT_MARKER_NAME, result);
    return result;
  }

  /**
   * @param id mutable side effect id
   * @param func given the value from the last marker returns value to store. If result is empty
   *     nothing is recorded into the history.
   * @return the latest value returned by func
   */
  Optional<byte[]> mutableSideEffect(
      String id, DataConverter converter, Func1<Optional<byte[]>, Optional<byte[]>> func) {
    decisions.addAllMissingVersionMarker(false, Optional.empty());
    return mutableSideEffectHandler.handle(id, converter, func);
  }

  public static class LocalActivityMarkerData {
    String activityId;
    String activityType;
    String errReason;
    byte[] errJson;
    byte[] result;
    long replayTime;
    int attempt;
    Duration backoff;

    public LocalActivityMarkerData(
        String activityId, String activityType, RespondActivityTaskCompletedRequest result) {
      this.activityId = activityId;
      this.activityType = activityType;
      this.result = result.getResult();
    }

    public LocalActivityMarkerData(
        String activityId, String activityType, RespondActivityTaskFailedRequest result) {
      this.activityId = activityId;
      this.activityType = activityType;
      this.errReason = result.getReason();
      this.errJson = result.getDetails();
    }

    public LocalActivityMarkerData(
        String activityId, String activityType, RespondActivityTaskCanceledRequest result) {
      this.activityId = activityId;
      this.activityType = activityType;
    }
  }

  void handleMarkerRecorded(HistoryEvent event) {
    MarkerRecordedEventAttributes attributes = event.getMarkerRecordedEventAttributes();
    String name = attributes.getMarkerName();
    if (SIDE_EFFECT_MARKER_NAME.equals(name)) {
      sideEffectResults.put(event.getEventId(), attributes.getDetails());
    } else if (LOCAL_ACTIVITY_MARKER_NAME.equals(name)) {
      handleLocalActivityMarker(attributes);
    } else if (!MUTABLE_SIDE_EFFECT_MARKER_NAME.equals(name) && !VERSION_MARKER_NAME.equals(name)) {
      log.warn("Unexpected marker: " + event);
    }
  }

  void handleLocalActivityMarker(MarkerRecordedEventAttributes attributes) {
    LocalActivityMarkerData marker =
        JsonDataConverter.getInstance()
            .fromData(
                attributes.getDetails(),
                LocalActivityMarkerData.class,
                LocalActivityMarkerData.class);

    if (pendingLaTasks.containsKey(marker.activityId)) {
      decisions.recordMarker(LOCAL_ACTIVITY_MARKER_NAME, attributes.getDetails());

      OpenRequestInfo<byte[], ActivityType> scheduled = pendingLaTasks.remove(marker.activityId);

      ActivityFailureException failure = null;
      if (marker.errJson != null) {
        Throwable cause =
            JsonDataConverter.getInstance()
                .fromData(marker.errJson, Throwable.class, Throwable.class);
        ActivityType activityType = new ActivityType();
        activityType.setName(marker.activityType);
        failure =
            new ActivityFailureException(
                attributes.getDecisionTaskCompletedEventId(),
                activityType,
                marker.activityId,
                cause);
      }

      BiConsumer<byte[], Exception> completionHandle = scheduled.getCompletionCallback();
      completionHandle.accept(marker.result, failure);
    }
  }

  int getVersion(String changeId, DataConverter converter, int minSupported, int maxSupported) {
    Predicate<byte[]> changeIdEquals =
        (bytesInEvent) -> {
          MarkerData markerData =
              converter.fromData(bytesInEvent, MarkerData.class, MarkerData.class);
          return markerData.getId().equals(changeId);
        };
    decisions.addAllMissingVersionMarker(true, Optional.of(changeIdEquals));

    Optional<byte[]> result =
        versionHandler.handle(
            changeId,
            converter,
            (stored) -> {
              if (stored.isPresent()) {
                return Optional.empty();
              }
              return Optional.of(converter.toData(maxSupported));
            });

    if (!result.isPresent()) {
      return WorkflowInternal.DEFAULT_VERSION;
    }
    int version = converter.fromData(result.get(), Integer.class, Integer.class);
    validateVersion(changeId, version, minSupported, maxSupported);
    return version;
  }

  private void validateVersion(String changeID, int version, int minSupported, int maxSupported) {
    if (version < minSupported || version > maxSupported) {
      throw new Error(
          String.format(
              "Version %d of changeID %s is not supported. Supported version is between %d and %d.",
              version, changeID, minSupported, maxSupported));
    }
  }

  Consumer<Exception> scheduleLocalActivityTask(
      ExecuteActivityParameters params, BiConsumer<byte[], Exception> callback) {
    final OpenRequestInfo<byte[], ActivityType> context =
        new OpenRequestInfo<>(params.getActivityType());
    context.setCompletionHandle(callback);
    pendingLaTasks.put(params.getActivityId(), context);
    unstartedLaTasks.add(params);
    return null;
  }

  void startUnstartedLaTasks() {
    for (ExecuteActivityParameters params : unstartedLaTasks) {
      laPollTask.offer(new LocalActivityWorker.Task(params, replayDecider));
    }
    unstartedLaTasks.clear();
  }

  boolean hasPendingLaTasks() {
    return !pendingLaTasks.isEmpty();
  }
}
