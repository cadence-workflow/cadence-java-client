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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.uber.cadence.FailureCategory;
import com.uber.cadence.FailureOptions;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.DataConverterException;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.workflow.ApplicationException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;

public class FailureConverterTest {

  private static final DataConverter JSON = JsonDataConverter.getInstance();

  public static class Payload {
    public String name;
    public int count;

    public Payload() {}

    Payload(String name, int count) {
      this.name = name;
      this.count = count;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Payload)) {
        return false;
      }
      Payload that = (Payload) o;
      return count == that.count && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, count);
    }
  }

  public static class CustomException extends RuntimeException {
    private final int code;

    public CustomException(String message, int code) {
      super(message);
      this.code = code;
    }
  }

  /**
   * Delegates to JSON without being a {@link JsonDataConverter}, like a user's custom converter.
   */
  private static class WrappingConverter implements DataConverter {
    @Override
    public byte[] toData(Object... value) throws DataConverterException {
      return JSON.toData(value);
    }

    @Override
    public <T> T fromData(byte[] content, Class<T> valueClass, Type valueType)
        throws DataConverterException {
      return JSON.fromData(content, valueClass, valueType);
    }

    @Override
    public Object[] fromDataArray(byte[] content, Type... valueType) throws DataConverterException {
      return JSON.fromDataArray(content, valueType);
    }
  }

  private static Throwable roundTrip(Throwable failure, DataConverter converter) {
    FailureConverter.EncodedFailure encoded = FailureConverter.encode(failure, converter);
    return FailureConverter.decode(
        encoded.getReason(), encoded.getDetails(), encoded.getFailureOptions(), converter);
  }

  @Test
  public void legacyExceptionRoundTrip() {
    for (DataConverter converter : new DataConverter[] {JSON, new WrappingConverter()}) {
      IllegalStateException failure = new IllegalStateException("outer", new IOException("inner"));
      FailureConverter.EncodedFailure encoded = FailureConverter.encode(failure, converter);
      assertEquals(IllegalStateException.class.getName(), encoded.getReason());
      assertNull(encoded.getFailureOptions());

      Throwable decoded =
          FailureConverter.decode(encoded.getReason(), encoded.getDetails(), null, converter);
      assertEquals(IllegalStateException.class, decoded.getClass());
      assertEquals("outer", decoded.getMessage());
      assertEquals(IOException.class, decoded.getCause().getClass());
      assertEquals("inner", decoded.getCause().getMessage());
    }
  }

  @Test
  public void legacyCustomExceptionKeepsFields() {
    Throwable decoded = roundTrip(new CustomException("boom", 42), JSON);
    assertEquals(CustomException.class, decoded.getClass());
    assertEquals(42, ((CustomException) decoded).code);
  }

  @Test
  public void checkedExceptionWrapperIsUnwrapped() {
    FailureConverter.EncodedFailure encoded =
        FailureConverter.encode(CheckedExceptionWrapper.wrap(new IOException("io")), JSON);
    assertEquals(IOException.class.getName(), encoded.getReason());
  }

  @Test
  public void applicationExceptionEncoding() {
    Payload payload = new Payload("a", 1);
    ApplicationException failure =
        new ApplicationException("PaymentDeclined", payload)
            .withFailureCategory(FailureCategory.Fatal)
            .withNextRetryDelay(Duration.ofMillis(1500));
    FailureConverter.EncodedFailure encoded = FailureConverter.encode(failure, JSON);

    assertEquals("PaymentDeclined", encoded.getReason());
    assertArrayEquals(JSON.toData(payload), encoded.getDetails());
    assertEquals(FailureCategory.Fatal, encoded.getFailureOptions().getFailureCategory());
    // Rounded up to whole seconds.
    assertEquals(2, encoded.getFailureOptions().getNextRetryIntervalSeconds());
  }

  @Test
  public void applicationExceptionRoundTrip() {
    for (DataConverter converter : new DataConverter[] {JSON, new WrappingConverter()}) {
      Payload payload = new Payload("a", 1);
      ApplicationException failure =
          new ApplicationException("PaymentDeclined", payload)
              .withFailureCategory(FailureCategory.Standard)
              .withNextRetryDelay(Duration.ofSeconds(5));

      Throwable decoded = roundTrip(failure, converter);
      assertTrue(decoded instanceof ApplicationException);
      ApplicationException e = (ApplicationException) decoded;
      assertEquals("PaymentDeclined", e.getReason());
      assertEquals("PaymentDeclined", e.getMessage());
      assertEquals(
          payload, converter.fromData(e.getDetailsBytes(converter), Payload.class, Payload.class));
      assertEquals(FailureCategory.Standard, e.getFailureCategory());
      assertEquals(Duration.ofSeconds(5), e.getNextRetryDelay());
    }
  }

  @Test
  public void applicationExceptionWithoutDetailsOrOptions() {
    ApplicationException decoded =
        (ApplicationException) roundTrip(new ApplicationException("NoDetails"), JSON);
    assertEquals("NoDetails", decoded.getReason());
    assertNull(decoded.getDetailsBytes(JSON));
    assertNull(decoded.getFailureCategory());
    assertNull(decoded.getNextRetryDelay());
  }

  @Test
  public void applicationExceptionWithMultipleDetails() {
    ApplicationException decoded =
        (ApplicationException) roundTrip(new ApplicationException("Multi", "a", 2), JSON);
    Object[] details =
        JSON.fromDataArray(decoded.getDetailsBytes(JSON), String.class, Integer.class);
    assertEquals("a", details[0]);
    assertEquals(2, details[1]);
  }

  @Test
  public void applicationExceptionWithGenericDetails() {
    Map<String, Payload> payload = ImmutableMap.of("k", new Payload("v", 3));
    ApplicationException decoded =
        (ApplicationException) roundTrip(new ApplicationException("Generic", payload), JSON);
    @SuppressWarnings({"unchecked", "rawtypes"})
    Map<String, Payload> result =
        JSON.fromData(
            decoded.getDetailsBytes(JSON),
            Map.class,
            new TypeToken<Map<String, Payload>>() {}.getType());
    assertEquals(payload, result);
  }

  @Test
  public void decodedApplicationExceptionIsReencodedAsIs() {
    byte[] details = "{\"go\":\"details\"}".getBytes(StandardCharsets.UTF_8);
    Throwable decoded = FailureConverter.decode("GoReason", details, null, JSON);
    FailureConverter.EncodedFailure encoded = FailureConverter.encode(decoded, JSON);
    assertEquals("GoReason", encoded.getReason());
    assertArrayEquals(details, encoded.getDetails());
  }

  /**
   * The risky case: an {@link ApplicationException} reason that is also an exception class name.
   * Its details don't look like a serialized exception, so it must not be decoded as one.
   */
  @Test
  public void applicationExceptionWithClassNameReason() {
    String reason = IllegalStateException.class.getName();
    Object[] detailsVariants = {
      "a string", new Payload("p", 1), ImmutableMap.of("detailMessage", "not an exception"), 42,
    };
    for (Object details : detailsVariants) {
      ApplicationException decoded =
          (ApplicationException) roundTrip(new ApplicationException(reason, details), JSON);
      assertEquals(reason, decoded.getReason());
    }
    ApplicationException noDetails =
        (ApplicationException) roundTrip(new ApplicationException(reason), JSON);
    assertEquals(reason, noDetails.getReason());
  }

  @Test
  public void jsonObjectWithMismatchedClassIsNotLegacy() {
    String reason = IllegalStateException.class.getName();
    byte[] details =
        ("{\"class\":\"" + IOException.class.getName() + "\",\"detailMessage\":\"m\"}")
            .getBytes(StandardCharsets.UTF_8);
    assertTrue(
        FailureConverter.decode(reason, details, null, JSON) instanceof ApplicationException);
  }

  @Test
  public void unknownReasonDecodesAsApplicationException() {
    // For example a CustomError reported by a Go worker.
    byte[] details = "\"some details\"".getBytes(StandardCharsets.UTF_8);
    for (DataConverter converter : new DataConverter[] {JSON, new WrappingConverter()}) {
      Throwable decoded =
          FailureConverter.decode(
              "cadenceInternal:Generic",
              details,
              new FailureOptions().setFailureCategory(FailureCategory.Poll),
              converter);
      assertTrue(decoded instanceof ApplicationException);
      ApplicationException e = (ApplicationException) decoded;
      assertEquals("cadenceInternal:Generic", e.getReason());
      assertEquals(
          "some details",
          converter.fromData(e.getDetailsBytes(converter), String.class, String.class));
      assertEquals(FailureCategory.Poll, e.getFailureCategory());
      assertNull(e.getNextRetryDelay());
    }
  }

  @Test
  public void nonJsonDetailsDecodeAsApplicationException() {
    byte[] details = new byte[] {0, 1, 2, (byte) 0xff};
    Throwable decoded =
        FailureConverter.decode(IllegalStateException.class.getName(), details, null, JSON);
    assertTrue(decoded instanceof ApplicationException);
    assertArrayEquals(details, ((ApplicationException) decoded).getDetailsBytes(JSON));
  }

  @Test
  public void customConverterFallbackKeepsConversionError() {
    Throwable decoded =
        FailureConverter.decode(
            IllegalStateException.class.getName(),
            "\"a string\"".getBytes(StandardCharsets.UTF_8),
            null,
            new WrappingConverter());
    assertTrue(decoded instanceof ApplicationException);
    assertEquals(1, decoded.getSuppressed().length);
    assertTrue(decoded.getSuppressed()[0] instanceof DataConverterException);
  }

  @Test
  public void reasonNamingNonThrowableClassIsNotLegacy() {
    byte[] details = JSON.toData("x");
    Throwable decoded = FailureConverter.decode(String.class.getName(), details, null, JSON);
    assertTrue(decoded instanceof ApplicationException);
  }

  @Test
  public void nullReasonAndDetails() {
    Throwable decoded = FailureConverter.decode(null, null, null, JSON);
    assertTrue(decoded instanceof ApplicationException);
    assertEquals("", ((ApplicationException) decoded).getReason());
  }

  @Test
  public void legacyEncodingIsUnchanged() {
    // Guards the wire format older clients rely on.
    RuntimeException failure = new RuntimeException("legacy");
    FailureConverter.EncodedFailure encoded = FailureConverter.encode(failure, JSON);
    assertEquals(RuntimeException.class.getName(), encoded.getReason());
    assertArrayEquals(JSON.toData(failure), encoded.getDetails());
  }

  @Test
  public void failureOptionsConversion() {
    assertNull(FailureConverter.toFailureOptions(new ApplicationException("r")));
    FailureOptions options =
        FailureConverter.toFailureOptions(
            new ApplicationException("r").withNextRetryDelay(Duration.ofSeconds(3)));
    assertFalse(options.isSetFailureCategory());
    assertEquals(3, options.getNextRetryIntervalSeconds());
  }
}
