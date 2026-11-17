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
