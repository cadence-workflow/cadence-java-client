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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.uber.cadence.FailureOptions;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.DataConverterException;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.workflow.ApplicationException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Converts exceptions to failure reason, details and options and back.
 *
 * <p>Exceptions other than {@link ApplicationException} use the "legacy" encoding: the reason is
 * the exception class name and the details are the exception serialized by the {@link
 * DataConverter}. {@link ApplicationException} uses its own reason and serialized details.
 *
 * <p>As there is no explicit marker on the wire, decoding has to tell the two encodings apart. A
 * failure is decoded using the legacy encoding only when its reason names a {@link Throwable} class
 * and its details look like that serialized exception. With {@link JsonDataConverter} the details
 * must be a JSON object with a "class" field equal to the reason, which is what the legacy encoding
 * always produces. With other converters the details must deserialize into the reason class.
 * Everything else is decoded as an {@link ApplicationException}.
 */
public final class FailureConverter {

  private static final String CLASS_FIELD = "class";

  public static final class EncodedFailure {
    private final String reason;
    private final byte[] details;
    private final FailureOptions failureOptions;

    EncodedFailure(String reason, byte[] details, FailureOptions failureOptions) {
      this.reason = reason;
      this.details = details;
      this.failureOptions = failureOptions;
    }

    public String getReason() {
      return reason;
    }

    public byte[] getDetails() {
      return details;
    }

    /** @return failure options or null if none were specified. */
    public FailureOptions getFailureOptions() {
      return failureOptions;
    }
  }

  private FailureConverter() {}

  /** Encodes a failure. Unwraps {@link CheckedExceptionWrapper} before encoding. */
  public static EncodedFailure encode(Throwable failure, DataConverter dataConverter) {
    failure = CheckedExceptionWrapper.unwrap(failure);
    if (failure instanceof ApplicationException) {
      ApplicationException e = (ApplicationException) failure;
      return new EncodedFailure(
          e.getReason(), e.getDetailsBytes(dataConverter), toFailureOptions(e));
    }
    return new EncodedFailure(failure.getClass().getName(), dataConverter.toData(failure), null);
  }

  /**
   * Decodes a failure into an exception. Never throws, deserialization errors are returned as the
   * resulting exception, or attached to it as suppressed exceptions.
   *
   * @param failureOptions failure options or null if not available
   */
  public static Throwable decode(
      String reason, byte[] details, FailureOptions failureOptions, DataConverter dataConverter) {
    Class<? extends Throwable> legacyClass = loadThrowableClass(reason);
    DataConverterException conversionFailure = null;
    if (legacyClass != null) {
      if (dataConverter instanceof JsonDataConverter) {
        if (isLegacyJson(reason, details)) {
          try {
            return dataConverter.fromData(details, legacyClass, legacyClass);
          } catch (Exception e) {
            return e;
          }
        }
      } else {
        // Can't inspect the encoding, so try the legacy one.
        try {
          Throwable result = dataConverter.fromData(details, legacyClass, legacyClass);
          if (result != null) {
            return result;
          }
        } catch (DataConverterException e) {
          conversionFailure = e;
        } catch (Exception e) {
          conversionFailure = new DataConverterException(e);
        }
      }
    }
    ApplicationException result = ApplicationException.fromEncodedDetails(reason, details);
    applyFailureOptions(result, failureOptions);
    if (conversionFailure != null) {
      result.addSuppressed(conversionFailure);
    }
    return result;
  }

  static FailureOptions toFailureOptions(ApplicationException e) {
    if (e.getFailureCategory() == null && e.getNextRetryDelay() == null) {
      return null;
    }
    FailureOptions result = new FailureOptions();
    if (e.getFailureCategory() != null) {
      result.setFailureCategory(e.getFailureCategory());
    }
    if (e.getNextRetryDelay() != null) {
      long seconds = OptionsUtils.roundUpToSeconds(e.getNextRetryDelay()).getSeconds();
      result.setNextRetryIntervalSeconds((int) Math.min(seconds, Integer.MAX_VALUE));
    }
    return result;
  }

  static void applyFailureOptions(ApplicationException e, FailureOptions options) {
    if (options == null) {
      return;
    }
    if (options.isSetFailureCategory()) {
      e.withFailureCategory(options.getFailureCategory());
    }
    if (options.isSetNextRetryIntervalSeconds()) {
      e.withNextRetryDelay(Duration.ofSeconds(options.getNextRetryIntervalSeconds()));
    }
  }

  private static Class<? extends Throwable> loadThrowableClass(String reason) {
    if (reason == null || reason.isEmpty()) {
      return null;
    }
    try {
      Class<?> c = Class.forName(reason);
      if (Throwable.class.isAssignableFrom(c)) {
        @SuppressWarnings("unchecked")
        Class<? extends Throwable> result = (Class<? extends Throwable>) c;
        return result;
      }
    } catch (Exception | LinkageError e) {
      // Not a legacy encoded failure.
    }
    return null;
  }

  /**
   * The legacy encoding always produces a JSON object with a "class" field equal to the reason. The
   * only exception is when the failure couldn't be serialized, in which case a {@link
   * DataConverterException} describing the problem is serialized instead.
   */
  private static boolean isLegacyJson(String reason, byte[] details) {
    if (details == null) {
      return false;
    }
    try {
      JsonElement element = JsonParser.parseString(new String(details, StandardCharsets.UTF_8));
      if (!element.isJsonObject()) {
        return false;
      }
      JsonObject object = element.getAsJsonObject();
      JsonElement classField = object.get(CLASS_FIELD);
      if (classField == null
          || !classField.isJsonPrimitive()
          || !classField.getAsJsonPrimitive().isString()) {
        return false;
      }
      String className = classField.getAsString();
      return className.equals(reason) || className.equals(DataConverterException.class.getName());
    } catch (RuntimeException e) {
      return false;
    }
  }
}
