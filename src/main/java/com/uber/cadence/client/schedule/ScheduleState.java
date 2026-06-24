// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence.client.schedule;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of a schedule's pause state returned by {@link
 * com.uber.cadence.client.WorkflowClient#describeSchedule}.
 */
public final class ScheduleState {

  private final boolean paused;
  private final String pauseReason;
  private final Instant pausedAt;
  private final String pausedBy;

  public ScheduleState(boolean paused, String pauseReason, Instant pausedAt, String pausedBy) {
    this.paused = paused;
    this.pauseReason = pauseReason;
    this.pausedAt = pausedAt;
    this.pausedBy = pausedBy;
  }

  /** Whether the schedule is currently paused. */
  public boolean isPaused() {
    return paused;
  }

  /**
   * Human-readable reason the schedule was paused. Non-null only when {@link #isPaused()} is {@code
   * true}.
   */
  public String getPauseReason() {
    return pauseReason;
  }

  /** When the schedule was paused. {@code null} when not paused. */
  public Instant getPausedAt() {
    return pausedAt;
  }

  /** Identity of the actor that paused the schedule. {@code null} when not paused. */
  public String getPausedBy() {
    return pausedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScheduleState)) return false;
    ScheduleState that = (ScheduleState) o;
    return paused == that.paused
        && Objects.equals(pauseReason, that.pauseReason)
        && Objects.equals(pausedAt, that.pausedAt)
        && Objects.equals(pausedBy, that.pausedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paused, pauseReason, pausedAt, pausedBy);
  }

  @Override
  public String toString() {
    return "ScheduleState{"
        + "paused="
        + paused
        + ", pauseReason='"
        + pauseReason
        + "', pausedAt="
        + pausedAt
        + ", pausedBy='"
        + pausedBy
        + "'}";
  }
}
