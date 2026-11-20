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

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

import com.uber.cadence.RetryPolicy;
import com.uber.cadence.common.RetryOptions;
import java.time.Duration;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class RetryParametersTest {

  private RetryParameters original;

  @Before
  public void setUp() {
    original = new RetryParameters();
    original.setMaximumIntervalInSeconds(120);
    original.setInitialIntervalInSeconds(10);
    original.setMaximumAttempts(5);
    original.setExpirationIntervalInSeconds(600);
    original.setBackoffCoefficient(2.0);
    original.setJitterCoefficient(0.2);
    original.setNonRetriableErrorReasons(Arrays.asList("Error1", "Error2"));
  }

  @Test
  public void testCopy() {
    RetryParameters copy = original.copy();

    // Verify that all properties are copied correctly
    assertEquals(original.getMaximumIntervalInSeconds(), copy.getMaximumIntervalInSeconds());
    assertEquals(original.getInitialIntervalInSeconds(), copy.getInitialIntervalInSeconds());
    assertEquals(original.getMaximumAttempts(), copy.getMaximumAttempts());
    assertEquals(original.getExpirationIntervalInSeconds(), copy.getExpirationIntervalInSeconds());
    assertEquals(original.getBackoffCoefficient(), copy.getBackoffCoefficient(), 0.0);
    assertEquals(original.getJitterCoefficient(), copy.getJitterCoefficient(), 0.0);
    assertEquals(original.getNonRetriableErrorReasons(), copy.getNonRetriableErrorReasons());

    // Verify that nonRetriableErrorReasons is deeply copied
    assertNotSame(original.getNonRetriableErrorReasons(), copy.getNonRetriableErrorReasons());
  }

  @Test
  public void testCopyWithNullNonRetriableErrorReasons() {
    original.setNonRetriableErrorReasons(null);

    RetryParameters copy = original.copy();

    // Check that properties are copied as expected
    assertNull(copy.getNonRetriableErrorReasons());
    assertEquals(original.getMaximumIntervalInSeconds(), copy.getMaximumIntervalInSeconds());
    assertEquals(original.getInitialIntervalInSeconds(), copy.getInitialIntervalInSeconds());
    assertEquals(original.getMaximumAttempts(), copy.getMaximumAttempts());
    assertEquals(original.getExpirationIntervalInSeconds(), copy.getExpirationIntervalInSeconds());
    assertEquals(original.getBackoffCoefficient(), copy.getBackoffCoefficient(), 0.0);
    assertEquals(original.getJitterCoefficient(), copy.getJitterCoefficient(), 0.0);
  }

  @Test
  public void testJitterCoefficientFromRetryOptionsToRetryPolicy() {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumAttempts(3)
            .setJitterCoefficient(0.2)
            .validateBuildWithDefaults();

    RetryParameters parameters = new RetryParameters(options);
    assertEquals(0.2, parameters.getJitterCoefficient(), 0.0);

    RetryPolicy policy = parameters.toRetryPolicy();
    assertEquals(0.2, policy.getJitterCoefficient(), 0.0);
  }
}
