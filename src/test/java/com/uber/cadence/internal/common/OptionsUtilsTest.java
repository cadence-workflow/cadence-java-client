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
package com.uber.cadence.internal.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OptionsUtilsTest {

  @Test
  public void testApplyJitter() {
    // {interval millis, jitter coefficient}
    double[][] testCases = {
      {
        4000, 0
      }, // zero jitter: the bounds collapse to the interval, so this asserts the exact value
      {4000, 0.2},
      {10000, 0.5},
      {2000, 1},
    };
    for (double[] tc : testCases) {
      long interval = (long) tc[0];
      double jitter = tc[1];
      long lower = (long) (interval * (1 - jitter));
      for (int i = 0; i < 100; i++) {
        long result = OptionsUtils.applyJitter(interval, jitter);
        assertTrue("jitter " + jitter + " result " + result, result >= lower && result <= interval);
      }
    }
  }

  /**
   * Out of bounds coefficients are rejected up front by RetryOptions and RetryState validation, so
   * applyJitter deliberately ignores them and returns the interval unchanged instead of throwing.
   */
  @Test
  public void testApplyJitterIgnoresOutOfBoundsCoefficient() {
    long interval = 4000;
    for (double jitter :
        new double[] {
          -0.1, -1, 1.1, 2, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        }) {
      assertEquals("jitter " + jitter, interval, OptionsUtils.applyJitter(interval, jitter));
    }
  }
}
