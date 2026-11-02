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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.uber.cadence.BadRequestError;
import com.uber.cadence.CancellationAlreadyRequestedError;
import com.uber.cadence.DomainAlreadyExistsError;
import com.uber.cadence.DomainNotActiveError;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.QueryFailedError;
import com.uber.cadence.ServiceBusyError;
import com.uber.cadence.WorkflowExecutionAlreadyCompletedError;
import com.uber.cadence.WorkflowExecutionAlreadyStartedError;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

public class RetryerTest {

  @Test
  public void testRpcEmptyOverridesPreserveDefaults() {
    RetryOptions defaults = RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS;
    assertEquals(
        defaults, RpcRetryer.mergeRetryOptions(new RetryOptions.Builder().build(), defaults, null));
    assertSame(defaults, RpcRetryer.mergeRetryOptions((RetryOptions) null, defaults, null));
  }

  @Test
  public void testRpcExclusionsReplaceDefaults() {
    RetryOptions defaults = RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS;
    assertEquals(
        new RetryOptions.Builder(defaults).setDoNotRetry(ServiceBusyError.class).build(),
        RpcRetryer.mergeRetryOptions(
            new RetryOptions.Builder().setDoNotRetry(ServiceBusyError.class).build(),
            defaults,
            null));
    assertEquals(
        new RetryOptions.Builder(defaults).setDoNotRetry().build(),
        RpcRetryer.mergeRetryOptions(
            new RetryOptions.Builder().setDoNotRetry().build(), defaults, null));
  }

  @Test
  public void testRpcOperationTimeouts() {
    RetryOptions operationDefaults =
        new RetryOptions.Builder(RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS)
            .setExpiration(Duration.ofMinutes(3))
            .build();
    RetryOptions overrides = new RetryOptions.Builder().setMaximumAttempts(1).build();
    assertEquals(
        Duration.ofMinutes(3),
        RpcRetryer.mergeRetryOptions(overrides, operationDefaults, Duration.ofMinutes(3))
            .getExpiration());

    overrides = new RetryOptions.Builder().setExpiration(Duration.ofMinutes(2)).build();
    assertEquals(
        Duration.ofMinutes(2),
        RpcRetryer.mergeRetryOptions(overrides, RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, null)
            .getExpiration());
    assertEquals(
        Duration.ofSeconds(3),
        RpcRetryer.mergeRetryOptions(overrides, operationDefaults, Duration.ofSeconds(3))
            .getExpiration());
    overrides = new RetryOptions.Builder().setExpiration(Duration.ofSeconds(1)).build();
    assertEquals(
        Duration.ofSeconds(1),
        RpcRetryer.mergeRetryOptions(overrides, operationDefaults, Duration.ofSeconds(3))
            .getExpiration());
  }

  @Test
  public void testRpcInheritedMaximumAccommodatesInitialInterval() {
    for (int seconds : new int[] {5, 7}) {
      RetryOptions overrides =
          new RetryOptions.Builder().setInitialInterval(Duration.ofSeconds(seconds)).build();
      RetryOptions actual =
          RpcRetryer.mergeRetryOptions(overrides, RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, null);
      assertEquals(Duration.ofSeconds(seconds), actual.getInitialInterval());
      assertEquals(Duration.ofSeconds(Math.max(6, seconds)), actual.getMaximumInterval());
      assertEquals(1.2, actual.getBackoffCoefficient(), 0);
    }
  }

  @Test
  public void testRpcEachFieldOverridesOnlyItself() {
    RetryOptions defaults = RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS;
    RetryOptions[] overrides = {
      new RetryOptions.Builder().setInitialInterval(Duration.ofMillis(500)).build(),
      new RetryOptions.Builder().setMaximumInterval(Duration.ofSeconds(10)).build(),
      new RetryOptions.Builder().setBackoffCoefficient(2.5).build(),
      new RetryOptions.Builder().setMaximumAttempts(1).build(),
      new RetryOptions.Builder().setExpiration(Duration.ofMinutes(2)).build()
    };
    for (int field = 0; field < overrides.length; field++) {
      RetryOptions actual = RpcRetryer.mergeRetryOptions(overrides[field], defaults, null);
      assertEquals(
          field == 0 ? overrides[field].getInitialInterval() : defaults.getInitialInterval(),
          actual.getInitialInterval());
      assertEquals(
          field == 1 ? overrides[field].getMaximumInterval() : defaults.getMaximumInterval(),
          actual.getMaximumInterval());
      assertEquals(
          field == 2 ? overrides[field].getBackoffCoefficient() : defaults.getBackoffCoefficient(),
          actual.getBackoffCoefficient(),
          0);
      assertEquals(
          field == 3 ? overrides[field].getMaximumAttempts() : defaults.getMaximumAttempts(),
          actual.getMaximumAttempts());
      assertEquals(
          field == 4 ? overrides[field].getExpiration() : defaults.getExpiration(),
          actual.getExpiration());
      assertEquals(defaults.getDoNotRetry(), actual.getDoNotRetry());
    }
  }

  @Test
  public void testRpcMissingAndUnsupportedServiceOptions() {
    IWorkflowService service = mock(IWorkflowService.class);
    assertSame(RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, RpcRetryer.mergeRetryOptions(service));
    when(service.getOptions()).thenThrow(new UnsupportedOperationException("legacy service"));
    assertSame(RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS, RpcRetryer.mergeRetryOptions(service));

    IWorkflowService brokenService = mock(IWorkflowService.class);
    IllegalStateException failure = new IllegalStateException("broken service");
    when(brokenService.getOptions()).thenThrow(failure);
    try {
      RpcRetryer.mergeRetryOptions(brokenService);
      fail("Unrelated failure must remain visible");
    } catch (IllegalStateException actual) {
      assertSame(failure, actual);
    }
  }

  @Test
  public void testExpiration() throws InterruptedException {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofMillis(10))
            .setMaximumInterval(Duration.ofMillis(100))
            .setExpiration(Duration.ofMillis(500))
            .setMaximumAttempts(20)
            .validateBuildWithDefaults();
    long start = System.currentTimeMillis();
    try {
      RpcRetryer.retryWithResultAsync(
              options,
              () -> {
                throw new IllegalArgumentException("simulated");
              })
          .get();
      fail("unreachable");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IllegalArgumentException);
      assertEquals("simulated", e.getCause().getMessage());
    }
    assertTrue(System.currentTimeMillis() - start > 500);
  }

  @Test
  public void testExpirationFuture() throws InterruptedException {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofMillis(10))
            .setMaximumInterval(Duration.ofMillis(100))
            .setExpiration(Duration.ofMillis(500))
            .validateBuildWithDefaults();
    long start = System.currentTimeMillis();
    try {
      RpcRetryer.retryWithResultAsync(
              options,
              () -> {
                CompletableFuture<Void> result = new CompletableFuture<>();
                result.completeExceptionally(new IllegalArgumentException("simulated"));
                return result;
              })
          .get();
      fail("unreachable");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IllegalArgumentException);
      assertEquals("simulated", e.getCause().getMessage());
    }
    assertTrue(System.currentTimeMillis() - start > 500);
  }

  @Test
  public void testInterruptedException() throws InterruptedException {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofMillis(10))
            .setMaximumInterval(Duration.ofMillis(100))
            .setExpiration(Duration.ofSeconds(100))
            .setDoNotRetry(InterruptedException.class)
            .validateBuildWithDefaults();
    long start = System.currentTimeMillis();
    try {
      RpcRetryer.retryWithResultAsync(
              options,
              () -> {
                CompletableFuture<Void> result = new CompletableFuture<>();
                result.completeExceptionally(new InterruptedException("simulated"));
                return result;
              })
          .get();
      fail("unreachable");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof InterruptedException);
      assertEquals("simulated", e.getCause().getMessage());
    }
    assertTrue(System.currentTimeMillis() - start < 100000);
  }

  @Test
  public void testAddDoNotRetry() throws InterruptedException {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofMillis(10))
            .setExpiration(Duration.ofSeconds(100))
            .validateBuildWithDefaults();
    options = options.addDoNotRetry(InterruptedException.class);
    // need to use array (or an object) since we cannot change the
    // value of the variable inside the lambda function.
    int[] numberOfCalls = {0};
    try {
      RpcRetryer.retryWithResultAsync(
              options,
              () -> {
                CompletableFuture<Void> result = new CompletableFuture<>();
                result.completeExceptionally(new InterruptedException("simulated"));
                ++numberOfCalls[0];
                return result;
              })
          .get();
      fail("unreachable");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof InterruptedException);
      assertEquals("simulated", e.getCause().getMessage());
    }
    // Make sure the error wasn't retried
    assertTrue(numberOfCalls[0] == 1);
  }

  @Test
  public void testMaxAttempt() throws InterruptedException {
    RetryOptions options =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofMillis(10))
            .setMaximumInterval(Duration.ofMillis(100))
            .setExpiration(Duration.ofMillis(500))
            .setMaximumAttempts(3)
            .validateBuildWithDefaults();
    // need to use array (or an object) since we cannot change the
    // value of the variable inside the lambda function.
    int[] numberOfCalls = {0};
    try {
      RpcRetryer.retryWithResultAsync(
              options,
              () -> {
                ++numberOfCalls[0];
                throw new IllegalArgumentException("simulated");
              })
          .get();
      fail("unreachable");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IllegalArgumentException);
      assertEquals("simulated", e.getCause().getMessage());
    }
    // Make sure the error wasn't retried
    assertTrue(numberOfCalls[0] == 3);
  }

  @Test
  public void testNonRetriableExceptionList() {
    // Since we tested retry and no-retry logic works correctly above,
    // In this test we just ensure the default options contain the right
    // set of Exceptions as non-retriable so we can make sure that any
    // change to that list would be intentional.

    List<Class<? extends Throwable>> noRetryExceptions =
        RpcRetryer.DEFAULT_RPC_RETRY_OPTIONS.getDoNotRetry();
    List<Class<? extends Throwable>> expectedList =
        new ArrayList<>(
            Arrays.asList(
                BadRequestError.class,
                EntityNotExistsError.class,
                WorkflowExecutionAlreadyCompletedError.class,
                WorkflowExecutionAlreadyStartedError.class,
                DomainAlreadyExistsError.class,
                QueryFailedError.class,
                DomainNotActiveError.class,
                CancellationAlreadyRequestedError.class));

    assertEquals(expectedList.size(), noRetryExceptions.size());
    for (Class<? extends Throwable> exp : noRetryExceptions) {
      assertTrue("Missing no retry exception in default options", expectedList.indexOf(exp) >= 0);
    }
  }
}
