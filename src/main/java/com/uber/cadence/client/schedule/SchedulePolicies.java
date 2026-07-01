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

import java.time.Duration;
import java.util.Objects;

/**
 * Configures schedule behavior: overlap, catch-up, and failure-handling policies.
 *
 * <p>Construct via {@link #newBuilder()}:
 *
 * <pre>{@code
 * SchedulePolicies policies = SchedulePolicies.newBuilder()
 *     .setOverlapPolicy(ScheduleOverlapPolicy.SKIP_NEW)
 *     .setCatchUpPolicy(ScheduleCatchUpPolicy.ONE)
 *     .setPauseOnFailure(true)
 *     .build();
 * }</pre>
 *
 * <h3>Overlap policy behavior on update</h3>
 *
 * <p>Policy changes on {@link com.uber.cadence.client.ScheduleClient#updateSchedule} are <em>not
 * retroactive</em>:
 *
 * <ul>
 *   <li>CONCURRENT → SKIP_NEW/CANCEL/TERMINATE: only the LastStartedWorkflow is checked on the next
 *       fire; other concurrent runs keep running untracked.
 *   <li>CONCURRENT unbounded → bounded ({@code concurrencyLimit} = N): existing runs are not
 *       counted against the new limit; cap is enforced for new fires only.
 *   <li>BUFFER → any: the buffered queue is cleared immediately; no buffered fires carry over.
 *   <li>any → BUFFER: buffering starts accumulating from the next fire onward.
 * </ul>
 */
public final class SchedulePolicies {

  private final ScheduleOverlapPolicy overlapPolicy;
  private final ScheduleCatchUpPolicy catchUpPolicy;
  private final Duration catchUpWindow;
  private final boolean pauseOnFailure;
  private final int bufferLimit;
  private final int concurrencyLimit;

  private SchedulePolicies(Builder b) {
    this.overlapPolicy = b.overlapPolicy;
    this.catchUpPolicy = b.catchUpPolicy;
    this.catchUpWindow = b.catchUpWindow;
    this.pauseOnFailure = b.pauseOnFailure;
    this.bufferLimit = b.bufferLimit;
    this.concurrencyLimit = b.concurrencyLimit;
  }

  /** What to do when a new run is scheduled while the previous is still running. */
  public ScheduleOverlapPolicy getOverlapPolicy() {
    return overlapPolicy;
  }

  /**
   * How to handle missed runs on unpause or recovery. Defaults to server-configured behavior when
   * {@code null}.
   */
  public ScheduleCatchUpPolicy getCatchUpPolicy() {
    return catchUpPolicy;
  }

  /**
   * Maximum look-back window for missed runs. Runs older than this are skipped even if the catch-up
   * policy is {@link ScheduleCatchUpPolicy#ALL}. {@code null} defers to the server default
   * (configurable via dynamic config).
   */
  public Duration getCatchUpWindow() {
    return catchUpWindow;
  }

  /**
   * Whether to automatically pause the schedule when a triggered workflow fails. Defaults to {@code
   * false}.
   */
  public boolean isPauseOnFailure() {
    return pauseOnFailure;
  }

  /**
   * Maximum number of runs queued when {@link ScheduleOverlapPolicy#BUFFER} is active. {@code 0}
   * means unlimited.
   */
  public int getBufferLimit() {
    return bufferLimit;
  }

  /**
   * Maximum number of concurrent runs when {@link ScheduleOverlapPolicy#CONCURRENT} is active.
   * {@code 0} means unlimited.
   */
  public int getConcurrencyLimit() {
    return concurrencyLimit;
  }

  /** Returns a new builder pre-populated with the values from this instance. */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /** Returns a new empty builder. */
  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SchedulePolicies)) return false;
    SchedulePolicies that = (SchedulePolicies) o;
    return pauseOnFailure == that.pauseOnFailure
        && bufferLimit == that.bufferLimit
        && concurrencyLimit == that.concurrencyLimit
        && overlapPolicy == that.overlapPolicy
        && catchUpPolicy == that.catchUpPolicy
        && Objects.equals(catchUpWindow, that.catchUpWindow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        overlapPolicy, catchUpPolicy, catchUpWindow, pauseOnFailure, bufferLimit, concurrencyLimit);
  }

  @Override
  public String toString() {
    return "SchedulePolicies{"
        + "overlapPolicy="
        + overlapPolicy
        + ", catchUpPolicy="
        + catchUpPolicy
        + ", catchUpWindow="
        + catchUpWindow
        + ", pauseOnFailure="
        + pauseOnFailure
        + ", bufferLimit="
        + bufferLimit
        + ", concurrencyLimit="
        + concurrencyLimit
        + '}';
  }

  public static final class Builder {

    private ScheduleOverlapPolicy overlapPolicy;
    private ScheduleCatchUpPolicy catchUpPolicy;
    private Duration catchUpWindow;
    private boolean pauseOnFailure;
    private int bufferLimit;
    private int concurrencyLimit;

    private Builder() {}

    private Builder(SchedulePolicies src) {
      this.overlapPolicy = src.overlapPolicy;
      this.catchUpPolicy = src.catchUpPolicy;
      this.catchUpWindow = src.catchUpWindow;
      this.pauseOnFailure = src.pauseOnFailure;
      this.bufferLimit = src.bufferLimit;
      this.concurrencyLimit = src.concurrencyLimit;
    }

    /** @see SchedulePolicies#getOverlapPolicy() */
    public Builder setOverlapPolicy(ScheduleOverlapPolicy overlapPolicy) {
      this.overlapPolicy = overlapPolicy;
      return this;
    }

    /** @see SchedulePolicies#getCatchUpPolicy() */
    public Builder setCatchUpPolicy(ScheduleCatchUpPolicy catchUpPolicy) {
      this.catchUpPolicy = catchUpPolicy;
      return this;
    }

    /** @see SchedulePolicies#getCatchUpWindow() */
    public Builder setCatchUpWindow(Duration catchUpWindow) {
      this.catchUpWindow = catchUpWindow;
      return this;
    }

    /** @see SchedulePolicies#isPauseOnFailure() */
    public Builder setPauseOnFailure(boolean pauseOnFailure) {
      this.pauseOnFailure = pauseOnFailure;
      return this;
    }

    /** @see SchedulePolicies#getBufferLimit() */
    public Builder setBufferLimit(int bufferLimit) {
      this.bufferLimit = bufferLimit;
      return this;
    }

    /** @see SchedulePolicies#getConcurrencyLimit() */
    public Builder setConcurrencyLimit(int concurrencyLimit) {
      this.concurrencyLimit = concurrencyLimit;
      return this;
    }

    public SchedulePolicies build() {
      return new SchedulePolicies(this);
    }
  }
}
