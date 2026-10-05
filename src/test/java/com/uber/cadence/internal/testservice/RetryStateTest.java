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

package com.uber.cadence.internal.testservice;

import static org.junit.Assert.assertEquals;

import com.uber.cadence.FailureCategory;
import com.uber.cadence.FailureOptions;
import com.uber.cadence.RetryPolicy;
import java.util.Collections;
import org.junit.Test;

public class RetryStateTest {

  private static RetryState newRetryState(int maximumAttempts) throws Exception {
    return new RetryState(
        new RetryPolicy()
            .setInitialIntervalInSeconds(1)
            .setBackoffCoefficient(2)
            .setMaximumIntervalInSeconds(5)
            .setMaximumAttempts(maximumAttempts)
            .setNonRetriableErrorReasons(Collections.singletonList("NonRetriable")),
        0);
  }

  @Test
  public void computedBackoff() throws Exception {
    RetryState state = newRetryState(10);
    assertEquals(1, state.getBackoffIntervalInSeconds("reason", null, 0));
    assertEquals(2, state.getNextAttempt().getBackoffIntervalInSeconds("reason", null, 0));
  }

  @Test
  public void nextRetryIntervalOverridesBackoff() throws Exception {
    RetryState state = newRetryState(10);
    // Not capped by the maximum interval, same as the server.
    FailureOptions options = new FailureOptions().setNextRetryIntervalSeconds(30);
    assertEquals(30, state.getBackoffIntervalInSeconds("reason", options, 0));
    // Zero means not set.
    options = new FailureOptions().setNextRetryIntervalSeconds(0);
    assertEquals(1, state.getBackoffIntervalInSeconds("reason", options, 0));
  }

  @Test
  public void nextRetryIntervalDoesNotAffectWhetherRetried() throws Exception {
    FailureOptions options = new FailureOptions().setNextRetryIntervalSeconds(30);
    assertEquals(0, newRetryState(1).getBackoffIntervalInSeconds("reason", options, 0));
    assertEquals(0, newRetryState(10).getBackoffIntervalInSeconds("NonRetriable", options, 0));
  }

  @Test
  public void fatalIsNotRetried() throws Exception {
    RetryState state = newRetryState(10);
    FailureOptions options =
        new FailureOptions()
            .setFailureCategory(FailureCategory.Fatal)
            .setNextRetryIntervalSeconds(30);
    assertEquals(0, state.getBackoffIntervalInSeconds("reason", options, 0));
    options = new FailureOptions().setFailureCategory(FailureCategory.Standard);
    assertEquals(1, state.getBackoffIntervalInSeconds("reason", options, 0));
  }
}
