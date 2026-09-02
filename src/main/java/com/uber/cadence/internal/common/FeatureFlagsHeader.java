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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.uber.cadence.FeatureFlags;

/** Serializes {@link FeatureFlags} into the value of the cadence-client-feature-flags header. */
public final class FeatureFlagsHeader {

  // The server deserializes this header with a protobuf JSON unmarshaller that fails on any field
  // it doesn't know, and silently falls back to all flags disabled when it does. The accepted
  // field names are therefore defined by the api.v1.FeatureFlags proto message (including its
  // json_name pins), not by the Thrift struct's field names. Serializing through the generated
  // proto message with JsonFormat keeps the header aligned with that contract by construction.
  private static final JsonFormat.Printer PRINTER =
      JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace();

  public static String serialize(FeatureFlags featureFlags) {
    com.uber.cadence.api.v1.FeatureFlags proto =
        com.uber.cadence.api.v1.FeatureFlags.newBuilder()
            .setWorkflowExecutionAlreadyCompletedErrorEnabled(
                featureFlags.isWorkflowExecutionAlreadyCompletedErrorEnabled())
            .setAutoforwardingEnabled(featureFlags.isAutoForwardingEnabled())
            .build();
    try {
      return PRINTER.print(proto);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("failed to serialize feature flags header", e);
    }
  }

  private FeatureFlagsHeader() {}
}
