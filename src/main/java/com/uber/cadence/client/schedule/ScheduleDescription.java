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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Full description of a schedule as returned by {@link
 * com.uber.cadence.client.ScheduleClient#describeSchedule}.
 *
 * <p>To update a schedule, read the current configuration with {@link
 * com.uber.cadence.client.ScheduleClient#describeSchedule}, modify the relevant fields in a new
 * {@link com.uber.cadence.UpdateScheduleRequest}, then submit via {@link
 * com.uber.cadence.client.ScheduleClient#updateSchedule}.
 */
public final class ScheduleDescription {

  private final ScheduleSpec spec;
  private final ScheduleAction action;
  private final SchedulePolicies policies;
  private final ScheduleState state;
  private final ScheduleInfo info;
  private final Map<String, Object> memo;
  private final Map<String, Object> searchAttributes;

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
    this.memo =
        memo == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(memo));
    this.searchAttributes =
        searchAttributes == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(searchAttributes));
  }

  /** The trigger spec (cron, start/end times, jitter). */
  public ScheduleSpec getSpec() {
    return spec;
  }

  /** The action executed on each trigger. */
  public ScheduleAction getAction() {
    return action;
  }

  /** Overlap, catch-up, and failure-handling policies. */
  public SchedulePolicies getPolicies() {
    return policies;
  }

  /**
   * Current pause state. To pause or unpause the schedule, use {@link
   * com.uber.cadence.client.ScheduleClient#pauseSchedule} or {@link
   * com.uber.cadence.client.ScheduleClient#unpauseSchedule}.
   */
  public ScheduleState getState() {
    return state;
  }

  /** Runtime statistics (last run, next run, total runs, etc.). */
  public ScheduleInfo getInfo() {
    return info;
  }

  /**
   * Memo key/value pairs attached to the schedule itself (not to triggered workflows). Never null;
   * empty map when none.
   */
  public Map<String, Object> getMemo() {
    return memo;
  }

  /** Search attributes attached to the schedule. Never null; empty map when none. */
  public Map<String, Object> getSearchAttributes() {
    return searchAttributes;
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
