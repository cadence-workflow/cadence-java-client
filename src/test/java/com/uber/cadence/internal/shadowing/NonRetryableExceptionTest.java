/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.uber.cadence.internal.shadowing;

import static com.uber.cadence.shadower.Constants.ErrNonRetryableType;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NonRetryableExceptionTest {

  @Test
  public void testNonRetryableException_ExpectedCanonicalNameEqualsToIDL() {
    assertEquals(ErrNonRetryableType, NonRetryableException.class.getCanonicalName());
  }
}
