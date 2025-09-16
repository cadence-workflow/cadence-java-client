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
package com.uber.cadence.internal.compatibility.proto.mappers;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class Helpers {

  static DoubleValue fromDoubleValue(double v) {
    return DoubleValue.newBuilder().setValue(v).build();
  }

  static Timestamp unixNanoToTime(long t) {
    return Timestamps.fromNanos(t);
  }

  static Duration secondsToDuration(int d) {
    return Duration.newBuilder().setSeconds(d).build();
  }

  static int longToInt(long v) {
    return (int) v;
  }

  static FieldMask newFieldMask(List<String> fields) {
    return FieldMask.newBuilder().addAllPaths(fields).build();
  }

  static Duration daysToDuration(int days) {
    return Durations.fromDays(days);
  }

  static Map nullToEmpty(Map t) {
    return MoreObjects.firstNonNull(t, Collections.emptyMap());
  }

  static String nullToEmpty(String t) {
    return Strings.nullToEmpty(t);
  }

  static boolean nullToEmpty(boolean t) {
    return MoreObjects.firstNonNull(t, false);
  }

  static ByteString arrayToByteString(byte[] t) {
    if (t == null) {
      return null;
    }
    return ByteString.copyFrom(t);
  }

  static long toInt64Value(Int64Value v) {
    return v.getValue();
  }

  static long timeToUnixNano(Timestamp t) {
    return Timestamps.toNanos(t);
  }

  static int durationToDays(Duration d) {
    return (int) Durations.toDays(d);
  }

  static int durationToSeconds(Duration d) {
    return (int) Durations.toSeconds(d);
  }

  static byte[] byteStringToArray(ByteString t) {
    if (t == null || t.size() == 0) {
      return null;
    }
    return t.toByteArray();
  }
}
