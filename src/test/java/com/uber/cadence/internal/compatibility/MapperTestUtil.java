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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.junit.Assert;

/**
 * Utility that asserts all fields on a Thrift object are present other than a specified list of
 * fields. This ensures that any changes to the IDL will result in the test failing unless either
 * the test or mapper is updated.
 */
public class MapperTestUtil {

  public static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>>
      void assertNoMissingFields(M message) {
    assertNoMissingFields(message, findFieldsEnum(message));
  }

  public static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>>
      void assertNoMissingFields(M message, Class<E> fields) {
    Assert.assertEquals(
        "All fields expected to be set in " + message.getClass().getSimpleName(),
        Collections.emptySet(),
        getUnsetFields(message, fields));
  }

  public static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>> void assertMissingFields(
      M message, String... values) {
    assertMissingFields(message, findFieldsEnum(message), ImmutableSet.copyOf(values));
  }

  public static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>> void assertMissingFields(
      M message, Set<String> values) {
    assertMissingFields(message, findFieldsEnum(message), values);
  }

  public static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>> void assertMissingFields(
      M message, Class<E> fields, String... values) {
    assertMissingFields(message, fields, ImmutableSet.copyOf(values));
  }

  public static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>> void assertMissingFields(
      M message, Class<E> fields, Set<String> expected) {
    Assert.assertEquals(
        "Additional fields are unexpectedly not set in " + message.getClass().getSimpleName(),
        expected,
        getUnsetFields(message, fields));
  }

  private static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>>
      Set<String> getUnsetFields(M message, Class<E> fields) {
    return Arrays.stream(fields.getEnumConstants())
        .filter(field -> !message.isSet(field))
        .map(TFieldIdEnum::getFieldName)
        .collect(Collectors.toSet());
  }

  @SuppressWarnings("unchecked")
  private static <E extends Enum<E> & TFieldIdEnum, M extends TBase<M, E>> Class<E> findFieldsEnum(
      M message) {
    for (Class<?> declaredClass : message.getClass().getDeclaredClasses()) {
      if ("_Fields".equals(declaredClass.getSimpleName())) {
        return (Class<E>) declaredClass;
      }
    }
    throw new IllegalStateException(
        "Failed to find _Fields enum for " + message.getClass().getCanonicalName());
  }
}
