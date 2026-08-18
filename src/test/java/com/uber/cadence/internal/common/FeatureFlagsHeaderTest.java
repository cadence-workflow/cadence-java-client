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

package com.uber.cadence.internal.common;

import static org.junit.Assert.assertEquals;

import com.uber.cadence.FeatureFlags;
import org.junit.Test;

public class FeatureFlagsHeaderTest {

  // The server rejects the whole header when it carries a field the IDL doesn't define, so these
  // assertions are on the exact wire format rather than on the parsed flags.
  @Test
  public void testSerializeEnabledFlag() {
    assertEquals(
        "{\"WorkflowExecutionAlreadyCompletedErrorEnabled\":true}",
        FeatureFlagsHeader.serialize(
            new FeatureFlags().setWorkflowExecutionAlreadyCompletedErrorEnabled(true)));
  }

  @Test
  public void testSerializeUnsetFlags() {
    assertEquals(
        "{\"WorkflowExecutionAlreadyCompletedErrorEnabled\":false}",
        FeatureFlagsHeader.serialize(new FeatureFlags()));
  }
}
