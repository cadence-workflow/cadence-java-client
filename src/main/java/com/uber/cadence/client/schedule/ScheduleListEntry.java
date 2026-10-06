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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * A single entry returned by {@link com.uber.cadence.client.ScheduleClient#listSchedules}.
 *
 * <p>Contains only the data available from the visibility store. For full detail including
 * policies, runtime info, and pause details (reason, timestamp, who paused), call {@link
 * com.uber.cadence.client.ScheduleClient#describeSchedule}.
 */
public final class ScheduleListEntry {

  private final String scheduleId;
  private final String workflowType;
  private final boolean paused;
  private final String cronExpression;
  private final Map<String, byte[]> memo;
  private final Map<String, byte[]> searchAttributes;

  public ScheduleListEntry(
      String scheduleId,
      String workflowType,
      boolean paused,
      String cronExpression,
      Map<String, byte[]> memo,
      Map<String, byte[]> searchAttributes) {
    this.scheduleId = scheduleId;
    this.workflowType = workflowType;
    this.paused = paused;
    this.cronExpression = cronExpression;
    this.memo = memo;
    this.searchAttributes = searchAttributes;
  }

  /** The unique schedule identifier within the domain. */
  public String getScheduleId() {
    return scheduleId;
  }

  /** Workflow type configured in the schedule action. */
  public String getWorkflowType() {
    return workflowType;
  }

  /**
   * Whether the schedule is currently paused. This is the only pause field available from the list
   * endpoint; the visibility store does not record pause reason, timestamp, or who paused. Call
   * {@link com.uber.cadence.client.ScheduleClient#describeSchedule} for full pause details.
   */
  public boolean isPaused() {
    return paused;
  }

  /** Cron expression configured in the spec. */
  public String getCronExpression() {
    return cronExpression;
  }

  /**
   * Memo key/value pairs attached to the schedule, as raw serialized bytes from the visibility
   * store. May be null if no memo was set.
   */
  public Map<String, byte[]> getMemo() {
    return memo;
  }

  /**
   * User-defined search attributes attached to the schedule, as raw serialized bytes from the
   * visibility store. Scheduler-internal attributes (CadenceSchedule* keys) are stripped by the
   * server before this is returned. May be null if no user search attributes were set.
   */
  public Map<String, byte[]> getSearchAttributes() {
    return searchAttributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ScheduleListEntry)) {
      return false;
    }
    ScheduleListEntry that = (ScheduleListEntry) o;
    return paused == that.paused
        && Objects.equals(scheduleId, that.scheduleId)
        && Objects.equals(workflowType, that.workflowType)
        && Objects.equals(cronExpression, that.cronExpression)
        && byteMapEquals(memo, that.memo)
        && byteMapEquals(searchAttributes, that.searchAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        scheduleId,
        workflowType,
        paused,
        cronExpression,
        byteMapHashCode(memo),
        byteMapHashCode(searchAttributes));
  }

  @Override
  public String toString() {
    return "ScheduleListEntry{"
        + "scheduleId='"
        + scheduleId
        + "', workflowType='"
        + workflowType
        + "', cronExpression='"
        + cronExpression
        + "', paused="
        + paused
        + ", memo="
        + memo
        + ", searchAttributes="
        + searchAttributes
        + '}';
  }

  private static boolean byteMapEquals(Map<String, byte[]> a, Map<String, byte[]> b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null || a.size() != b.size()) {
      return false;
    }
    for (Map.Entry<String, byte[]> e : a.entrySet()) {
      if (!Arrays.equals(e.getValue(), b.get(e.getKey()))) {
        return false;
      }
    }
    return true;
  }

  private static int byteMapHashCode(Map<String, byte[]> m) {
    if (m == null) {
      return 0;
    }
    int h = 0;
    for (Map.Entry<String, byte[]> e : m.entrySet()) {
      h += Objects.hashCode(e.getKey()) ^ Arrays.hashCode(e.getValue());
    }
    return h;
  }
}
