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

import com.uber.cadence.FailureCategory;
import com.uber.cadence.converter.DataConverter;
import java.time.Duration;

/**
 * An exception that lets activity and workflow code explicitly control the failure reported to
 * Cadence.
 *
 * <p>By default, an exception thrown from an activity or workflow is reported with its class name
 * as the failure reason and the serialized exception as the failure details. When an {@code
 * ApplicationException} is thrown instead, the failure reason is {@link #getReason()} and the
 * failure details are the {@code details} passed to the constructor, serialized with the worker's
 * {@link DataConverter}. This makes failures readable by other Cadence clients (it is the Java
 * equivalent of the Go client's {@code CustomError}).
 *
 * <p>Activities may additionally specify a {@link FailureCategory} and a next retry delay, which
 * are sent to the Cadence server as failure options. For example {@link FailureCategory#Fatal}
 * prevents the server from retrying the activity regardless of its retry policy. They only apply to
 * activities retried by the server: they are ignored for local activities, workflow failures and
 * {@link Workflow#retry}, and are not available on exceptions decoded from those failures.
 *
 * <p>On the caller side, failures that were reported by an {@code ApplicationException}, as well as
 * failures whose reason is not a Java exception class name (for example failures reported by
 * workers written in other languages), are surfaced as an {@code ApplicationException}. For
 * example, as the cause of an {@link ActivityFailureException}.
 *
 * <pre><code>
 * throw new ApplicationException("PaymentDeclined", declineInfo)
 *     .withNextRetryDelay(Duration.ofMinutes(5));
 * </code></pre>
 *
 * <p>Note that callers running an older client version decode these failures the same way they
 * decode any failure with an unknown reason: as a {@link ClassNotFoundException} cause or a
 * deserialization error.
 */
public final class ApplicationException extends RuntimeException {

  private final String reason;

  // Set when the exception is created by user code. Serialized with the worker's DataConverter.
  private final Object[] details;

  // Set when the exception was decoded from a failure.
  private final byte[] encodedDetails;

  private FailureCategory failureCategory;
  private Duration nextRetryDelay;

  /**
   * @param reason failure reason reported to Cadence. Must not be null or empty.
   * @param details optional failure details. Serialized using the worker's {@link DataConverter}.
   */
  public ApplicationException(String reason, Object... details) {
    super(validateReason(reason));
    this.reason = reason;
    this.details = details == null ? new Object[0] : details;
    this.encodedDetails = null;
  }

  private ApplicationException(String reason, byte[] encodedDetails) {
    super(reason);
    this.reason = reason;
    this.details = null;
    this.encodedDetails = encodedDetails;
  }

  private static String validateReason(String reason) {
    if (reason == null || reason.isEmpty()) {
      throw new IllegalArgumentException("reason must not be null or empty");
    }
    return reason;
  }

  /** Creates an exception from already serialized details. Used when decoding failures. */
  public static ApplicationException fromEncodedDetails(String reason, byte[] encodedDetails) {
    return new ApplicationException(reason == null ? "" : reason, encodedDetails);
  }

  /**
   * Sets the failure category reported to the Cadence server. Only applies to activity failures.
   *
   * @return this exception
   */
  public ApplicationException withFailureCategory(FailureCategory failureCategory) {
    this.failureCategory = failureCategory;
    return this;
  }

  /**
   * Overrides the delay before the next retry of the failed activity computed from its retry
   * policy. It doesn't affect whether the activity is retried. The delay is rounded up to whole
   * seconds. Only applies to activity failures.
   *
   * @return this exception
   */
  public ApplicationException withNextRetryDelay(Duration nextRetryDelay) {
    if (nextRetryDelay != null && nextRetryDelay.isNegative()) {
      throw new IllegalArgumentException("negative nextRetryDelay: " + nextRetryDelay);
    }
    this.nextRetryDelay = nextRetryDelay;
    return this;
  }

  /** @return failure reason. */
  public String getReason() {
    return reason;
  }

  /** @return failure category or null if not specified. */
  public FailureCategory getFailureCategory() {
    return failureCategory;
  }

  /** @return next retry delay or null if not specified. */
  public Duration getNextRetryDelay() {
    return nextRetryDelay;
  }

  /**
   * Returns the serialized failure details. Deserialize them with the same converter, for example
   * {@code converter.fromData(e.getDetailsBytes(converter), Foo.class, Foo.class)}.
   *
   * @param converter converter used to serialize the details if this exception wasn't decoded from
   *     a failure. Exceptions decoded from a failure return the details exactly as they were
   *     received and ignore it.
   * @return serialized details or null if there are no details
   */
  public byte[] getDetailsBytes(DataConverter converter) {
    if (encodedDetails != null || details == null) {
      return encodedDetails;
    }
    return converter.toData(details);
  }
}
