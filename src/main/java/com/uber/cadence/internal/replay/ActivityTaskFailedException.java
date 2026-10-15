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

package com.uber.cadence.internal.replay;

import com.uber.cadence.ActivityType;
import com.uber.cadence.FailureOptions;

/**
 * Internal. Do not catch or throw in application level code. Exception used to communicate failure
 * of remote activity. TODO: Make package level visibility.
 */
@SuppressWarnings("serial")
public class ActivityTaskFailedException extends RuntimeException {

  private final long eventId;
  private final ActivityType activityType;
  private final String activityId;
  private final byte[] details;
  private final String reason;
  private final FailureOptions failureOptions;

  ActivityTaskFailedException(
      long eventId,
      ActivityType activityType,
      String activityId,
      String reason,
      byte[] details,
      FailureOptions failureOptions) {
    super(reason);
    this.eventId = eventId;
    this.activityType = activityType;
    this.activityId = activityId;
    this.reason = reason;
    this.details = details;
    this.failureOptions = failureOptions;
  }

  public long getEventId() {
    return eventId;
  }

  public ActivityType getActivityType() {
    return activityType;
  }

  public String getActivityId() {
    return activityId;
  }

  public byte[] getDetails() {
    return details;
  }

  public String getReason() {
    return reason;
  }

  /** @return failure options or null if not set. */
  public FailureOptions getFailureOptions() {
    return failureOptions;
  }
}
