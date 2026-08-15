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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uber.cadence.FeatureFlags;
import java.lang.reflect.Modifier;

/** Serializes {@link FeatureFlags} into the value of the cadence-client-feature-flags header. */
public final class FeatureFlagsHeader {

  // The server deserializes this header with a protobuf JSON unmarshaller that fails on any field
  // it doesn't know, and silently falls back to all flags disabled when it does. Thrift declares
  // the IDL fields public and keeps its own bookkeeping, such as __isset_bitfield, private, so
  // excluding private fields leaves exactly the fields the server expects. Field names are sent as
  // Thrift declares them because the proto IDL pins json_name to those names.
  private static final Gson GSON =
      new GsonBuilder()
          .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.TRANSIENT)
          .create();

  public static String serialize(FeatureFlags featureFlags) {
    return GSON.toJson(featureFlags);
  }

  private FeatureFlagsHeader() {}
}
