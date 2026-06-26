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
 *     desc.setPolicies(desc.getPolicies().toBuilder()
 *         .setOverlapPolicy(ScheduleOverlapPolicy.SKIP_NEW)
 *         .build());
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
   * Replaces the memo on the schedule.
   *
   * @see com.uber.cadence.client.WorkflowClient#updateSchedule
   */
  public ScheduleDescription setMemo(Map<String, Object> memo) {
    this.memo = memo;
    return this;
  }

  /** Search attributes attached to the schedule. */
  public Map<String, Object> getSearchAttributes() {
    return searchAttributes;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScheduleDescription)) return false;
    ScheduleDescription that = (ScheduleDescription) o;
    return Objects.equals(spec, that.spec)
        && Objects.equals(action, that.action)
        && Objects.equals(policies, that.policies)
        && Objects.equals(state, that.state)
        && Objects.equals(info, that.info)
        && Objects.equals(memo, that.memo)
        && Objects.equals(searchAttributes, that.searchAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, action, policies, state, info, memo, searchAttributes);
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
        + ", info="
        + info
        + ", memo="
        + memo
        + ", searchAttributes="
        + searchAttributes
        + '}';
  }
}
