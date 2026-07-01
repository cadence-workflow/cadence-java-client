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
package com.uber.cadence.client.schedule;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable runtime statistics for a schedule returned by {@link
 * com.uber.cadence.client.ScheduleClient#describeSchedule}.
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
    this.ongoingBackfills =
        ongoingBackfills == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(ongoingBackfills));
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScheduleInfo)) return false;
    ScheduleInfo that = (ScheduleInfo) o;
    return totalRuns == that.totalRuns
        && missedRuns == that.missedRuns
        && skippedRuns == that.skippedRuns
        && Objects.equals(lastRunTime, that.lastRunTime)
        && Objects.equals(nextRunTime, that.nextRunTime)
        && Objects.equals(createTime, that.createTime)
        && Objects.equals(lastUpdateTime, that.lastUpdateTime)
        && Objects.equals(ongoingBackfills, that.ongoingBackfills);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        lastRunTime,
        nextRunTime,
        totalRuns,
        createTime,
        lastUpdateTime,
        ongoingBackfills,
        missedRuns,
        skippedRuns);
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
        + ", lastUpdateTime="
        + lastUpdateTime
        + ", ongoingBackfills="
        + ongoingBackfills
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
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof BackfillInfo)) return false;
      BackfillInfo that = (BackfillInfo) o;
      return runsCompleted == that.runsCompleted
          && runsTotal == that.runsTotal
          && Objects.equals(backfillId, that.backfillId)
          && Objects.equals(startTime, that.startTime)
          && Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
      return Objects.hash(backfillId, startTime, endTime, runsCompleted, runsTotal);
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
