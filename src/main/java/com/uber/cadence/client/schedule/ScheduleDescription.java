// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence.client.schedule;

import java.util.Map;
import java.util.Objects;

/**
 * Full description of a schedule as returned by {@link
 * com.uber.cadence.client.WorkflowClient#describeSchedule}.
 *
 * <p>This object is also the input to the {@code updater} function passed to {@link
 * com.uber.cadence.client.WorkflowClient#updateSchedule}. Mutations applied inside the updater are
 * collected and sent to the server atomically as part of the describe-first read-modify-write
 * cycle.
 *
 * <pre>{@code
 * client.updateSchedule(scheduleId, desc -> {
 *     desc.setSpec(desc.getSpec().toBuilder()
 *         .setCronExpression("0 0,6,12,18 * * *")
 *         .build());
 *     desc.getPolicies().toBuilder()
 *         .setOverlapPolicy(ScheduleOverlapPolicy.SKIP_NEW);
 *     return desc;
 * });
 * }</pre>
 */
public final class ScheduleDescription {

  private ScheduleSpec spec;
  private ScheduleAction action;
  private SchedulePolicies policies;
  private ScheduleState state;
  private ScheduleInfo info;
  private Map<String, Object> memo;
  private Map<String, Object> searchAttributes;

  public ScheduleDescription(
      ScheduleSpec spec,
      ScheduleAction action,
      SchedulePolicies policies,
      ScheduleState state,
      ScheduleInfo info,
      Map<String, Object> memo,
      Map<String, Object> searchAttributes) {
    this.spec = spec;
    this.action = action;
    this.policies = policies;
    this.state = state;
    this.info = info;
    this.memo = memo;
    this.searchAttributes = searchAttributes;
  }

  /** The trigger spec (cron, start/end times, jitter). */
  public ScheduleSpec getSpec() {
    return spec;
  }

  /**
   * Replaces the spec. Call this inside an updater to change when the schedule fires.
   *
   * @see com.uber.cadence.client.WorkflowClient#updateSchedule
   */
  public ScheduleDescription setSpec(ScheduleSpec spec) {
    this.spec = Objects.requireNonNull(spec, "spec");
    return this;
  }

  /** The action executed on each trigger. */
  public ScheduleAction getAction() {
    return action;
  }

  /**
   * Replaces the action. Call this inside an updater to change what workflow is started.
   *
   * @see com.uber.cadence.client.WorkflowClient#updateSchedule
   */
  public ScheduleDescription setAction(ScheduleAction action) {
    this.action = Objects.requireNonNull(action, "action");
    return this;
  }

  /** Overlap, catch-up, and failure-handling policies. */
  public SchedulePolicies getPolicies() {
    return policies;
  }

  /**
   * Replaces the policies. Call this inside an updater to change overlap or catch-up behavior.
   *
   * @see com.uber.cadence.client.WorkflowClient#updateSchedule
   */
  public ScheduleDescription setPolicies(SchedulePolicies policies) {
    this.policies = Objects.requireNonNull(policies, "policies");
    return this;
  }

  /**
   * Current pause state. Read-only; use {@link
   * com.uber.cadence.client.WorkflowClient#pauseSchedule} to pause.
   */
  public ScheduleState getState() {
    return state;
  }

  /** Runtime statistics (last run, next run, total runs, etc.). */
  public ScheduleInfo getInfo() {
    return info;
  }

  /** Memo key/value pairs attached to the schedule itself (not to triggered workflows). */
  public Map<String, Object> getMemo() {
    return memo;
  }

  /**
   * Replaces the search attributes on the schedule.
   *
   * @see com.uber.cadence.client.WorkflowClient#updateSchedule
   */
  public ScheduleDescription setSearchAttributes(Map<String, Object> searchAttributes) {
    this.searchAttributes = searchAttributes;
    return this;
  }

  /** Search attributes attached to the schedule. */
  public Map<String, Object> getSearchAttributes() {
    return searchAttributes;
  }

  @Override
  public String toString() {
    return "ScheduleDescription{"
        + "spec="
        + spec
        + ", action="
        + action
        + ", policies="
        + policies
        + ", state="
        + state
        + '}';
  }
}
