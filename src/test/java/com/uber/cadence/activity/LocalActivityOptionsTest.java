/*
 *
 *  *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  *
 *  *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  *  use this file except in compliance with the License. A copy of the License is
 *  *  located at
 *  *
 *  *  http://aws.amazon.com/apache2.0
 *  *
 *  *  or in the "license" file accompanying this file. This file is distributed on
 *  *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  *  express or implied. See the License for the specific language governing
 *  *  permissions and limitations under the License.
 *
 */

package com.uber.cadence.activity;

import static org.junit.Assert.assertNotEquals;

import com.uber.cadence.common.RetryOptions;
import java.time.Duration;
import junit.framework.TestCase;
import org.junit.Test;

public class LocalActivityOptionsTest extends TestCase {

  @Test
  public void testTestToString() {
    String asString = createOptions().toString();

    final String expected =
        "LocalActivityOptions{"
            + "scheduleToCloseTimeout=PT2M3S, "
            + "retryOptions=RetryOptions{"
            + "initialInterval=PT2M3S, "
            + "backoffCoefficient=0.0, "
            + "jitterCoefficient=0.0, "
            + "expiration=null, "
            + "maximumAttempts=43, "
            + "maximumInterval=null, "
            + "doNotRetry=null"
            + "}}";

    assertEquals(expected, asString);
  }

  @Test
  public void testTestHashCode() {
    LocalActivityOptions options1 = createOptions(123);
    LocalActivityOptions options2 = createOptions(456);

    // The hash code of different objects should be different
    assertNotEquals(options1.hashCode(), options2.hashCode());
  }

  public static LocalActivityOptions createOptions() {
    return createOptions(123);
  }

  public static LocalActivityOptions createOptions(int initialRetryInterval) {
    RetryOptions retryOptions =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofSeconds(initialRetryInterval))
            .setMaximumAttempts(43)
            .build();

    return new LocalActivityOptions.Builder()
        .setScheduleToCloseTimeout(Duration.ofSeconds(123))
        .setRetryOptions(retryOptions)
        .build();
  }
}
