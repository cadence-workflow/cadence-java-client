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

package com.uber.cadence.workflow;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.uber.cadence.FailureCategory;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import java.time.Duration;
import org.junit.Test;

public class ApplicationExceptionTest {

  @Test
  public void getters() {
    ApplicationException e =
        new ApplicationException("reason", "details")
            .withFailureCategory(FailureCategory.Fatal)
            .withNextRetryDelay(Duration.ofSeconds(10));
    assertEquals("reason", e.getReason());
    assertEquals("reason", e.getMessage());
    assertEquals(FailureCategory.Fatal, e.getFailureCategory());
    assertEquals(Duration.ofSeconds(10), e.getNextRetryDelay());
  }

  @Test
  public void defaults() {
    ApplicationException e = new ApplicationException("reason");
    assertNull(e.getFailureCategory());
    assertNull(e.getNextRetryDelay());
    assertNull(e.getDetailsBytes(JsonDataConverter.getInstance()));
  }

  @Test
  public void detailsBytes() {
    DataConverter converter = JsonDataConverter.getInstance();
    assertArrayEquals(
        converter.toData("a", 1),
        new ApplicationException("reason", "a", 1).getDetailsBytes(converter));
  }

  @Test
  public void fromEncodedDetails() {
    byte[] details = new byte[] {1, 2, 3};
    ApplicationException e = ApplicationException.fromEncodedDetails("reason", details);
    assertEquals("reason", e.getReason());
    // Decoded details are returned as received, regardless of the converter.
    assertArrayEquals(details, e.getDetailsBytes(JsonDataConverter.getInstance()));
    assertArrayEquals(details, e.getDetailsBytes(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullReason() {
    new ApplicationException(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyReason() {
    new ApplicationException("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeDelay() {
    new ApplicationException("reason").withNextRetryDelay(Duration.ofSeconds(-1));
  }
}
