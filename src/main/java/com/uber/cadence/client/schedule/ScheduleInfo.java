// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence.client.schedule;

import java.time.Instant;
import java.util.List;

/**
 * Immutable runtime statistics for a schedule returned by {@link
 * com.uber.cadence.client.WorkflowClient#describeSchedule}.
 */
public final class ScheduleInfo {

  private final Instant lastRunTime;
  private final Instant nextRunTime;
  private final long totalRuns;
  private final Instant createTime;
  private final Instant lastUpdateTime;
  private final List<BackfillInfo> ongoingBackfills;
  private final long missedRuns;
  private final long skippedRuns;

  public ScheduleInfo(
      Instant lastRunTime,
      Instant nextRunTime,
      long totalRuns,
      Instant createTime,
      Instant lastUpdateTime,
      List<BackfillInfo> ongoingBackfills,
      long missedRuns,
      long skippedRuns) {
    this.lastRunTime = lastRunTime;
    this.nextRunTime = nextRunTime;
    this.totalRuns = totalRuns;
    this.createTime = createTime;
    this.lastUpdateTime = lastUpdateTime;
    this.ongoingBackfills = ongoingBackfills;
    this.missedRuns = missedRuns;
    this.skippedRuns = skippedRuns;
  }

  /** When the last workflow was triggered. {@code null} if the schedule has never fired. */
  public Instant getLastRunTime() {
    return lastRunTime;
  }

  /** When the next workflow will be triggered. */
  public Instant getNextRunTime() {
    return nextRunTime;
  }

  /** Total number of workflows started by this schedule (regular, catch-up, and backfill). */
  public long getTotalRuns() {
    return totalRuns;
  }

  /** When the schedule was created. */
  public Instant getCreateTime() {
    return createTime;
  }

  /** When the schedule was last updated. */
  public Instant getLastUpdateTime() {
    return lastUpdateTime;
  }

  /** Currently active backfill operations. Empty when no backfill is in progress. */
  public List<BackfillInfo> getOngoingBackfills() {
    return ongoingBackfills;
  }

  /** Number of missed runs that were skipped by the catch-up policy (e.g. due to downtime). */
  public long getMissedRuns() {
    return missedRuns;
  }

  /**
   * Number of runs skipped due to the overlap policy (e.g. {@link ScheduleOverlapPolicy#SKIP_NEW}).
   */
  public long getSkippedRuns() {
    return skippedRuns;
  }

  @Override
  public String toString() {
    return "ScheduleInfo{"
        + "lastRunTime="
        + lastRunTime
        + ", nextRunTime="
        + nextRunTime
        + ", totalRuns="
        + totalRuns
        + ", createTime="
        + createTime
        + ", missedRuns="
        + missedRuns
        + ", skippedRuns="
        + skippedRuns
        + '}';
  }

  /** Progress of an ongoing backfill operation. */
  public static final class BackfillInfo {

    private final String backfillId;
    private final Instant startTime;
    private final Instant endTime;
    private final int runsCompleted;
    private final int runsTotal;

    public BackfillInfo(
        String backfillId, Instant startTime, Instant endTime, int runsCompleted, int runsTotal) {
      this.backfillId = backfillId;
      this.startTime = startTime;
      this.endTime = endTime;
      this.runsCompleted = runsCompleted;
      this.runsTotal = runsTotal;
    }

    /** Client-provided or server-assigned backfill identifier. */
    public String getBackfillId() {
      return backfillId;
    }

    /** Start of the backfill time range. */
    public Instant getStartTime() {
      return startTime;
    }

    /** End of the backfill time range. */
    public Instant getEndTime() {
      return endTime;
    }

    /** Number of runs completed so far. */
    public int getRunsCompleted() {
      return runsCompleted;
    }

    /** Total number of runs in this backfill range. */
    public int getRunsTotal() {
      return runsTotal;
    }

    @Override
    public String toString() {
      return "BackfillInfo{"
          + "backfillId='"
          + backfillId
          + "', startTime="
          + startTime
          + ", endTime="
          + endTime
          + ", runsCompleted="
          + runsCompleted
          + ", runsTotal="
          + runsTotal
          + '}';
    }
  }
}
