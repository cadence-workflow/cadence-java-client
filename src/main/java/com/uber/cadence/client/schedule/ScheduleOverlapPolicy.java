// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence.client.schedule;

/**
 * Defines what happens when a new scheduled run is triggered while a previous one is still running.
 *
 * <p>Behavior is not retroactive on update: existing runs keep running under the old policy; only
 * new fires observe the updated policy. See {@link
 * com.uber.cadence.client.WorkflowClient#updateSchedule} for detail.
 */
public enum ScheduleOverlapPolicy {
  /**
   * Skip the new run if the previous workflow is still running. This is the default.
   *
   * <p>Equivalent to proto {@code SCHEDULE_OVERLAP_POLICY_SKIP_NEW}.
   */
  SKIP_NEW,

  /**
   * Buffer new runs and execute them sequentially after the current run completes.
   *
   * <p>The maximum queue depth is controlled by {@link SchedulePolicies#getBufferLimit()}. A limit
   * of 0 means unlimited. When updating from BUFFER to any other policy the queued runs are
   * dropped.
   *
   * <p>Equivalent to proto {@code SCHEDULE_OVERLAP_POLICY_BUFFER}.
   */
  BUFFER,

  /**
   * Allow multiple runs to execute concurrently with no ordering guarantee.
   *
   * <p>The maximum concurrency is controlled by {@link SchedulePolicies#getConcurrencyLimit()}. A
   * limit of 0 means unlimited.
   *
   * <p>Equivalent to proto {@code SCHEDULE_OVERLAP_POLICY_CONCURRENT}.
   */
  CONCURRENT,

  /**
   * Cancel the previous run gracefully, then start the new one.
   *
   * <p>Equivalent to proto {@code SCHEDULE_OVERLAP_POLICY_CANCEL_PREVIOUS}.
   */
  CANCEL_PREVIOUS,

  /**
   * Terminate the previous run immediately, then start the new one.
   *
   * <p>Equivalent to proto {@code SCHEDULE_OVERLAP_POLICY_TERMINATE_PREVIOUS}.
   */
  TERMINATE_PREVIOUS,
}
