// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence.client.schedule;

/**
 * Defines how missed runs are handled when a schedule is unpaused or when the server recovers from
 * downtime.
 *
 * <p>Catch-up runs are still subject to the configured {@link ScheduleOverlapPolicy}: if the buffer
 * or concurrency limit is reached, excess catch-up runs are dropped.
 *
 * <p>The catch-up window (maximum look-back horizon) is configured separately via {@link
 * SchedulePolicies#getCatchUpWindow()}.
 */
public enum ScheduleCatchUpPolicy {
  /**
   * Skip all missed runs. Only future runs will execute.
   *
   * <p>Equivalent to proto {@code SCHEDULE_CATCH_UP_POLICY_SKIP}.
   */
  SKIP,

  /**
   * Execute only the single most-recently missed scheduled time, skipping all others.
   *
   * <p>Equivalent to proto {@code SCHEDULE_CATCH_UP_POLICY_ONE}.
   */
  ONE,

  /**
   * Execute a run for every missed scheduled time within the catch-up window.
   *
   * <p>Equivalent to proto {@code SCHEDULE_CATCH_UP_POLICY_ALL}.
   */
  ALL,
}
