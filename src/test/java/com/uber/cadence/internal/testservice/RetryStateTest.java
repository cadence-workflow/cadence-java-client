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
