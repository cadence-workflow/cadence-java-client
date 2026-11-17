package com.uber.cadence.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.time.Duration;
import org.junit.Test;

public class RetryOptionsTest {

  private static RetryOptions.Builder builder() {
    return new RetryOptions.Builder()
        .setInitialInterval(Duration.ofSeconds(1))
        .setMaximumAttempts(3);
  }

  @Test
  public void testJitterCoefficientDefaultsToZero() {
    assertEquals(0d, builder().validateBuildWithDefaults().getJitterCoefficient(), 0.0);
  }

  @Test
  public void testValidJitterCoefficients() {
    for (double jitter : new double[] {0, 0.2, 1}) {
      RetryOptions options = builder().setJitterCoefficient(jitter).validateBuildWithDefaults();
      assertEquals(jitter, options.getJitterCoefficient(), 0.0);
    }
  }

  @Test
  public void testInvalidJitterCoefficients() {
    for (double jitter : new double[] {-0.1, 1.1, Double.NaN}) {
      try {
        builder().setJitterCoefficient(jitter);
        fail("expected IllegalArgumentException for jitter " + jitter);
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test
  public void testEqualsAndHashCodeIncludeJitterCoefficient() {
    RetryOptions a = builder().setJitterCoefficient(0.1).validateBuildWithDefaults();
    RetryOptions b = builder().setJitterCoefficient(0.1).validateBuildWithDefaults();
    RetryOptions c = builder().setJitterCoefficient(0.2).validateBuildWithDefaults();

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
  }
}
