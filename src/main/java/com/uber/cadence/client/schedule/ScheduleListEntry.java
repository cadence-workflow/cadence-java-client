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

import java.util.Objects;

/**
 * A single entry returned by {@link com.uber.cadence.client.ScheduleClient#listSchedules}.
 *
 * <p>Contains only the data available from the visibility store. For full detail including policies
 * and runtime info, call {@link com.uber.cadence.client.ScheduleClient#describeSchedule}.
 */
public final class ScheduleListEntry {

  private final String scheduleId;
  private final String workflowType;
  private final ScheduleState state;
  private final String cronExpression;

  public ScheduleListEntry(
      String scheduleId, String workflowType, ScheduleState state, String cronExpression) {
    this.scheduleId = scheduleId;
    this.workflowType = workflowType;
    this.state = state;
    this.cronExpression = cronExpression;
  }

  /** The unique schedule identifier within the domain. */
  public String getScheduleId() {
    return scheduleId;
  }

  /** Workflow type configured in the schedule action. */
  public String getWorkflowType() {
    return workflowType;
  }

  /** Current pause state of the schedule. */
  public ScheduleState getState() {
    return state;
  }

  /** Cron expression configured in the spec. */
  public String getCronExpression() {
    return cronExpression;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScheduleListEntry)) return false;
    ScheduleListEntry that = (ScheduleListEntry) o;
    return Objects.equals(scheduleId, that.scheduleId)
        && Objects.equals(workflowType, that.workflowType)
        && Objects.equals(state, that.state)
        && Objects.equals(cronExpression, that.cronExpression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scheduleId, workflowType, state, cronExpression);
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
        + (state != null && state.isPaused())
        + '}';
  }
}
