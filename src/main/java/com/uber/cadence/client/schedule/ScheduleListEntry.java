// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence.client.schedule;

/**
 * A single entry returned by {@link com.uber.cadence.client.WorkflowClient#listSchedules}.
 *
 * <p>Contains only the data available from the visibility store. For full detail including policies
 * and runtime info, call {@link com.uber.cadence.client.WorkflowClient#describeSchedule}.
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
