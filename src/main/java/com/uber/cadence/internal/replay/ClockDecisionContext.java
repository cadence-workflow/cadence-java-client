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

import com.uber.cadence.HistoryEvent;
import com.uber.cadence.MarkerRecordedEventAttributes;
import com.uber.cadence.StartTimerDecisionAttributes;
import com.uber.cadence.TimerCanceledEventAttributes;
import com.uber.cadence.TimerFiredEventAttributes;
import com.uber.cadence.workflow.Functions.Func;
import com.uber.cadence.workflow.Functions.Func1;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Clock that must be used inside workflow definition code to ensure replay determinism. */
final class ClockDecisionContext {

  private static final class MutableSideEffectEventIdDataPair {

    private final long eventId;
    private final byte[] data;

    MutableSideEffectEventIdDataPair(long eventId, byte[] data) {
      this.eventId = eventId;
      this.data = data;
    }

    long getEventId() {
      return eventId;
    }

    byte[] getData() {
      return data;
    }
  }

  private static final String SIDE_EFFECT_MARKER_NAME = "SideEffect";
  private static final String MUTABLE_SIDE_EFFECT_MARKER_NAME = "MutableSideEffect";

  private static final Logger log = LoggerFactory.getLogger(ReplayDecider.class);

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

  // key is startedEventId
  private final Map<Long, OpenRequestInfo<?, Long>> scheduledTimers = new HashMap<>();

  private long replayCurrentTimeMilliseconds;

  private boolean replaying = true;

  // Key is side effect marker eventId
  private final Map<Long, byte[]> sideEffectResults = new HashMap<>();
  // Key is mutableSideEffect id
  private final Map<String, MutableSideEffectEventIdDataPair> mutableSideEffectResults =
      new HashMap<>();

  ClockDecisionContext(DecisionsHelper decisions) {
    this.decisions = decisions;
  }

  long currentTimeMillis() {
    return replayCurrentTimeMilliseconds;
  }

  void setReplayCurrentTimeMilliseconds(long replayCurrentTimeMilliseconds) {
    this.replayCurrentTimeMilliseconds = replayCurrentTimeMilliseconds;
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
    timer.setTimerId(String.valueOf(decisions.getNextId()));
    long startEventId = decisions.startTimer(timer, null);
    context.setCompletionHandle((ctx, e) -> callback.accept(e));
    scheduledTimers.put(startEventId, context);
    return new ClockDecisionContext.TimerCancellationHandler(startEventId);
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
  Optional<byte[]> mutableSideEffect(String id, Func1<Optional<byte[]>, Optional<byte[]>> func) {
    MutableSideEffectEventIdDataPair pair = mutableSideEffectResults.get(id);
    Optional<byte[]> stored;
    if (pair == null) {
      stored = Optional.empty();
    } else {
      stored = Optional.of(pair.getData());
    }
    long eventId = decisions.getNextDecisionEventId();
    try {
      if (replaying) {
        if (stored.isPresent() && pair.getEventId() == eventId) {
          recordMutableSideEffectMarker(id, eventId, stored.get());
        }
        return stored;
      }
      Optional<byte[]> toStore = func.apply(stored);
      if (toStore.isPresent()) {
        byte[] data = toStore.get();
        recordMutableSideEffectMarker(id, eventId, data);
        return toStore;
      }
      return stored;
    } catch (Error e) {
      throw e;
    } catch (Exception e) {
      throw new Error("mutableSideEffect function failed", e);
    }
  }

  private void recordMutableSideEffectMarker(String id, long eventId, byte[] data)
      throws IOException {
    // dataConverter should not be used at this level.
    // So using DataOutputStream to pach both id and data.
    // Deserialized in handleMutableSideEffectMarker
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(bout);
    out.writeUTF(id);
    out.writeInt(data.length);
    out.write(data);
    mutableSideEffectResults.put(id, new MutableSideEffectEventIdDataPair(eventId, data));
    decisions.recordMarker(MUTABLE_SIDE_EFFECT_MARKER_NAME, bout.toByteArray());
  }

  void handleMarkerRecorded(HistoryEvent event) {
    MarkerRecordedEventAttributes attributes = event.getMarkerRecordedEventAttributes();
    String name = attributes.getMarkerName();
    if (SIDE_EFFECT_MARKER_NAME.equals(name)) {
      sideEffectResults.put(event.getEventId(), attributes.getDetails());
    } else if (MUTABLE_SIDE_EFFECT_MARKER_NAME.equals(name)) {
      handleMutableSideEffectMarker(event.getEventId(), attributes);
    } else {
      log.warn("Unexpected marker: " + event);
    }
  }

  /** Id and data are serialized into details in {@link #mutableSideEffect(String, Func1)}. */
  private void handleMutableSideEffectMarker(
      long eventId, MarkerRecordedEventAttributes attributes) {
    ByteArrayInputStream bin = new ByteArrayInputStream(attributes.getDetails());
    DataInputStream in = new DataInputStream(bin);
    try {
      String id = in.readUTF();
      int length = in.readInt();
      byte[] data = new byte[length];
      in.read(data);
      mutableSideEffectResults.put(id, new MutableSideEffectEventIdDataPair(eventId, data));
    } catch (IOException e) {
      throw new Error("Failure deserializing mutableSideEffect details", e);
    }
  }
}
