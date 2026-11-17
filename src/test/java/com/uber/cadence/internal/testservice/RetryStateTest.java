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
package com.uber.cadence.internal.testservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.uber.cadence.BadRequestError;
import com.uber.cadence.RetryPolicy;
import org.junit.Test;

public class RetryStateTest {

  private static RetryPolicy policy(double jitter) {
    return new RetryPolicy()
        .setInitialIntervalInSeconds(10)
        .setBackoffCoefficient(2)
        .setMaximumIntervalInSeconds(30)
        .setMaximumAttempts(20)
        .setJitterCoefficient(jitter);
  }

  @Test
  public void testBackoffWithoutJitter() throws BadRequestError {
    RetryState state = new RetryState(policy(0), 0);
    assertEquals(10, state.getBackoffIntervalInSeconds("error", 0));
    assertEquals(20, state.getNextAttempt().getBackoffIntervalInSeconds("error", 0));
  }

  @Test
  public void testBackoffWithJitter() throws BadRequestError {
    // attempt 0: 10s base; attempt 5: capped at the 30s maximum interval before jitter
    RetryState first = new RetryState(policy(0.5), 0);
    RetryState capped = first;
    for (int i = 0; i < 5; i++) {
      capped = capped.getNextAttempt();
    }
    for (int i = 0; i < 100; i++) {
      int backoff = first.getBackoffIntervalInSeconds("error", 0);
      assertTrue("backoff " + backoff, backoff >= 5 && backoff <= 10);
      int cappedBackoff = capped.getBackoffIntervalInSeconds("error", 0);
      assertTrue("capped backoff " + cappedBackoff, cappedBackoff >= 15 && cappedBackoff <= 30);
    }
  }

  @Test
  public void testFullJitterStillRetries() throws BadRequestError {
    RetryPolicy policy =
        new RetryPolicy()
            .setInitialIntervalInSeconds(1)
            .setBackoffCoefficient(1)
            .setMaximumAttempts(20)
            .setJitterCoefficient(1);
    RetryState state = new RetryState(policy, 0);
    for (int i = 0; i < 100; i++) {
      // 0 would mean no retry
      assertEquals(1, state.getBackoffIntervalInSeconds("error", 0));
    }
  }

  @Test
  public void testFractionalBackoffRoundsUpToWholeSeconds() throws BadRequestError {
    RetryPolicy policy =
        new RetryPolicy()
            .setInitialIntervalInSeconds(1)
            .setBackoffCoefficient(1.5)
            .setMaximumAttempts(20);
    RetryState state = new RetryState(policy, 0);
    assertEquals(1, state.getBackoffIntervalInSeconds("error", 0));
    // 1.5s rounds up to 2s rather than truncating to 1s
    assertEquals(2, state.getNextAttempt().getBackoffIntervalInSeconds("error", 0));
  }

  @Test
  public void testJitteredBackoffRoundsUpToWholeSeconds() throws BadRequestError {
    // 2s base with 0.2 jitter gives (1.6s, 2s], which must round up to 2s, never down to 1s
    RetryPolicy policy =
        new RetryPolicy()
            .setInitialIntervalInSeconds(2)
            .setBackoffCoefficient(1)
            .setMaximumAttempts(20)
            .setJitterCoefficient(0.2);
    RetryState state = new RetryState(policy, 0);
    for (int i = 0; i < 100; i++) {
      assertEquals(2, state.getBackoffIntervalInSeconds("error", 0));
    }
  }

  @Test
  public void testExpirationIsCheckedAgainstRoundedBackoff() throws BadRequestError {
    // attempt 1 backs off 1.5s, which rounds up to 2s
    RetryPolicy policy =
        new RetryPolicy()
            .setInitialIntervalInSeconds(1)
            .setBackoffCoefficient(1.5)
            .setMaximumAttempts(20)
            .setExpirationIntervalInSeconds(60);
    long now = 1_000_000;

    // 1.8s left: the unrounded 1.5s would fit, but the 2s that is actually used does not
    RetryState expiresBeforeRoundedBackoff = new RetryState(policy, now + 1800).getNextAttempt();
    assertEquals(0, expiresBeforeRoundedBackoff.getBackoffIntervalInSeconds("error", now));

    // 2s left: the rounded 2s backoff fits exactly
    RetryState expiresAtRoundedBackoff = new RetryState(policy, now + 2000).getNextAttempt();
    assertEquals(2, expiresAtRoundedBackoff.getBackoffIntervalInSeconds("error", now));
  }

  @Test
  public void testMinimumBackoffIsCheckedAgainstExpiration() throws BadRequestError {
    // full jitter can shrink the backoff to a few ms, but the 1s floor is what gets scheduled
    RetryPolicy policy =
        new RetryPolicy()
            .setInitialIntervalInSeconds(1)
            .setBackoffCoefficient(1)
            .setMaximumAttempts(20)
            .setExpirationIntervalInSeconds(60)
            .setJitterCoefficient(1);
    long now = 1_000_000;
    RetryState state = new RetryState(policy, now + 999);
    for (int i = 0; i < 100; i++) {
      assertEquals(0, state.getBackoffIntervalInSeconds("error", now));
    }
  }

  @Test
  public void testInvalidJitterCoefficientIsRejected() {
    for (double jitter : new double[] {-0.1, 1.1, Double.NaN}) {
      try {
        new RetryState(policy(jitter), 0);
        fail("expected BadRequestError for jitter " + jitter);
      } catch (BadRequestError e) {
        assertEquals("JitterCoefficient must be between 0 and 1 on retry policy.", e.getMessage());
      }
    }
  }
}
