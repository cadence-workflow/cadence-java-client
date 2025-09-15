/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.internal.worker;

import com.uber.cadence.CadenceError;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.Duration;
import com.uber.m3.util.ImmutableMap;

abstract class ActivityPollTaskBase implements Poller.PollTask<PollForActivityTaskResponse> {

  protected final SingleWorkerOptions options;

  public ActivityPollTaskBase(SingleWorkerOptions options) {
    this.options = options;
  }

  public PollForActivityTaskResponse poll() throws CadenceError {

    PollForActivityTaskResponse result = pollTask();
    if (result == null || result.getTaskToken() == null) {
      return null;
    }

    Scope metricsScope =
        options
            .getMetricsScope()
            .tagged(
                ImmutableMap.of(
                    MetricsTag.ACTIVITY_TYPE,
                    result.getActivityType().getName(),
                    MetricsTag.WORKFLOW_TYPE,
                    result.getWorkflowType().getName()));
    metricsScope.counter(MetricsType.ACTIVITY_POLL_SUCCEED_COUNTER).inc(1);
    metricsScope
        .timer(MetricsType.ACTIVITY_SCHEDULED_TO_START_LATENCY)
        .record(
            Duration.ofNanos(
                result.getStartedTimestamp() - result.getScheduledTimestampOfThisAttempt()));
    return result;
  }

  protected abstract PollForActivityTaskResponse pollTask() throws CadenceError;
}
