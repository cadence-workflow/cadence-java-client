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

package com.uber.cadence.internal.compatibility;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Assert;

/**
 * Utility that asserts all fields on a Thrift object are present other than a specified list of
 * fields. This ensures that any changes to the IDL will result in the test failing unless either
 * the test or mapper is updated.
 */
public class MapperTestUtil {
  public static void assertNoMissingFields(Object message) {
    Set<String> nullFields = getMissingFields(message.toString());

    Assert.assertEquals("All fields expected to be set in the text", new HashSet<>(), nullFields);
  }

  public static void assertMissingFields(Object message, Set<String> values) {
    Set<String> nullFields = getMissingFields(message.toString());
    Assert.assertEquals("Expected missing fields but get different", values, nullFields);
  }

  private static Set<String> getMissingFields(String text) {
    Set<String> nullFields = new HashSet<>();
    // Regex to find fieldName=null
    Pattern pattern = Pattern.compile("(\\w+)=null");
    Matcher matcher = pattern.matcher(text);

    while (matcher.find()) {
      nullFields.add(matcher.group(1)); // group(1) captures the field name
    }
    return nullFields;
  }
}
