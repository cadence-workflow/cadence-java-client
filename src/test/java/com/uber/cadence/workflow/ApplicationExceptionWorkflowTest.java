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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.uber.cadence.FailureCategory;
import com.uber.cadence.activity.Activity;
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.activity.LocalActivityOptions;
import com.uber.cadence.client.WorkflowFailureException;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.testUtils.CadenceTestRule;
import java.time.Duration;
import java.util.Objects;
import org.junit.Rule;
import org.junit.Test;

/**
 * Workflows assert on the failures they observe and throw {@link IllegalStateException} on a
 * mismatch, which fails the workflow. JUnit assertions throw an {@link Error}, which would only
 * fail the decision task and make the test time out.
 */
public class ApplicationExceptionWorkflowTest {

  private static final DataConverter CONVERTER = JsonDataConverter.getInstance();

  @Rule
  public CadenceTestRule testRule =
      CadenceTestRule.builder()
          .withWorkflowTypes(
              ApplicationExceptionWorkflowImpl.class,
              LegacyExceptionWorkflowImpl.class,
              FatalWorkflowImpl.class,
              NextRetryDelayWorkflowImpl.class,
              FailingWorkflowImpl.class,
              ParentWorkflowImpl.class)
          .withActivities(new TestActivitiesImpl())
          .startWorkersAutomatically()
          .build();

  public interface TestActivities {
    @ActivityMethod
    void throwApplicationException();

    @ActivityMethod
    void throwApplicationExceptionWithoutDetails();

    @ActivityMethod
    void throwLegacyException();

    @ActivityMethod
    void throwFatalException();

    @ActivityMethod
    int retryWithNextRetryDelay();
  }

  public static class TestActivitiesImpl implements TestActivities {
    @Override
    public void throwApplicationException() {
      throw new ApplicationException("ActivityReason", "activity details")
          .withFailureCategory(FailureCategory.Standard)
          .withNextRetryDelay(Duration.ofSeconds(3));
    }

    @Override
    public void throwApplicationExceptionWithoutDetails() {
      throw new ApplicationException("NoDetails");
    }

    @Override
    public void throwLegacyException() {
      throw new IllegalArgumentException("legacy failure");
    }

    @Override
    public void throwFatalException() {
      throw new ApplicationException("FatalReason", Activity.getTask().getAttempt())
          .withFailureCategory(FailureCategory.Fatal);
    }

    @Override
    public int retryWithNextRetryDelay() {
      int attempt = Activity.getTask().getAttempt();
      if (attempt == 0) {
        throw new ApplicationException("RetryLater", attempt)
            .withNextRetryDelay(Duration.ofHours(1));
      }
      return attempt;
    }
  }

  public interface ApplicationExceptionWorkflow {
    @WorkflowMethod
    void run();
  }

  public interface LegacyExceptionWorkflow {
    @WorkflowMethod
    void run();
  }

  public interface FatalWorkflow {
    @WorkflowMethod
    void run();
  }

  public interface NextRetryDelayWorkflow {
    @WorkflowMethod
    void run();
  }

  public interface FailingWorkflow {
    @WorkflowMethod(executionStartToCloseTimeoutSeconds = 20)
    void run();
  }

  public interface ParentWorkflow {
    @WorkflowMethod
    void run();
  }

  public static class ApplicationExceptionWorkflowImpl implements ApplicationExceptionWorkflow {
    @Override
    public void run() {
      TestActivities activities =
          newActivityStub(
              new RetryOptions.Builder()
                  .setInitialInterval(Duration.ofSeconds(1))
                  .setMaximumAttempts(1));
      ApplicationException e =
          expectApplicationException(() -> activities.throwApplicationException());
      expectEquals("reason", "ActivityReason", e.getReason());
      expectEquals("details", "activity details", details(e, String.class));
      expectEquals("failure category", FailureCategory.Standard, e.getFailureCategory());
      expectEquals("next retry delay", Duration.ofSeconds(3), e.getNextRetryDelay());

      // Failure options are not recorded for local activities.
      TestActivities localActivities = newLocalActivityStub();
      e = expectApplicationException(() -> localActivities.throwApplicationException());
      expectEquals("local activity reason", "ActivityReason", e.getReason());
      expectEquals("local activity details", "activity details", details(e, String.class));
      expectEquals("local activity failure category", null, e.getFailureCategory());
      expectEquals("local activity next retry delay", null, e.getNextRetryDelay());

      // Without details the failure is encoded with null details.
      e = expectApplicationException(() -> activities.throwApplicationExceptionWithoutDetails());
      expectEquals("reason without details", "NoDetails", e.getReason());
      expectEquals("details bytes", null, e.getDetailsBytes(CONVERTER));
      e =
          expectApplicationException(
              () -> localActivities.throwApplicationExceptionWithoutDetails());
      expectEquals("local activity reason without details", "NoDetails", e.getReason());
      expectEquals("local activity details bytes", null, e.getDetailsBytes(CONVERTER));
    }
  }

  public static class LegacyExceptionWorkflowImpl implements LegacyExceptionWorkflow {
    @Override
    public void run() {
      TestActivities activities =
          newActivityStub(
              new RetryOptions.Builder()
                  .setInitialInterval(Duration.ofSeconds(1))
                  .setMaximumAttempts(1));
      Throwable cause = expectActivityFailure(() -> activities.throwLegacyException());
      expectEquals("cause", IllegalArgumentException.class, cause.getClass());
      expectEquals("message", "legacy failure", cause.getMessage());

      TestActivities localActivities = newLocalActivityStub();
      cause = expectActivityFailure(() -> localActivities.throwLegacyException());
      expectEquals("local activity cause", IllegalArgumentException.class, cause.getClass());
      expectEquals("local activity message", "legacy failure", cause.getMessage());
    }
  }

  public static class FatalWorkflowImpl implements FatalWorkflow {
    @Override
    public void run() {
      TestActivities activities =
          newActivityStub(
              new RetryOptions.Builder()
                  .setInitialInterval(Duration.ofSeconds(1))
                  .setMaximumAttempts(5));
      ApplicationException e = expectApplicationException(() -> activities.throwFatalException());
      expectEquals("failure category", FailureCategory.Fatal, e.getFailureCategory());
      expectEquals("failed attempt", 0, details(e, Integer.class));
    }
  }

  /**
   * The retry policy alone would retry after a second and the second attempt would succeed. The
   * next retry delay pushes the retry past the expiration instead, so the activity isn't retried.
   */
  public static class NextRetryDelayWorkflowImpl implements NextRetryDelayWorkflow {
    @Override
    public void run() {
      TestActivities activities =
          newActivityStub(
              new RetryOptions.Builder()
                  .setInitialInterval(Duration.ofSeconds(1))
                  .setMaximumInterval(Duration.ofSeconds(1))
                  .setExpiration(Duration.ofSeconds(30))
                  .setMaximumAttempts(5));
      ApplicationException e;
      try {
        int attempt = activities.retryWithNextRetryDelay();
        throw new IllegalStateException(
            "retried on attempt " + attempt + ", next retry delay was ignored");
      } catch (ActivityFailureException failure) {
        e = expectApplicationException(failure.getCause());
      }
      expectEquals("reason", "RetryLater", e.getReason());
      expectEquals("failed attempt", 0, details(e, Integer.class));
    }
  }

  public static class FailingWorkflowImpl implements FailingWorkflow {
    @Override
    public void run() {
      throw new ApplicationException("WorkflowReason", "workflow details");
    }
  }

  public static class ParentWorkflowImpl implements ParentWorkflow {
    @Override
    public void run() {
      FailingWorkflow child = Workflow.newChildWorkflowStub(FailingWorkflow.class);
      ApplicationException e;
      try {
        child.run();
        throw new IllegalStateException("expected child workflow failure");
      } catch (ChildWorkflowFailureException failure) {
        e = expectApplicationException(failure.getCause());
      }
      expectEquals("reason", "WorkflowReason", e.getReason());
      expectEquals("details", "workflow details", details(e, String.class));
    }
  }

  private static TestActivities newActivityStub(RetryOptions.Builder retryOptions) {
    return Workflow.newActivityStub(
        TestActivities.class,
        new ActivityOptions.Builder()
            .setScheduleToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(retryOptions.build())
            .build());
  }

  private static TestActivities newLocalActivityStub() {
    return Workflow.newLocalActivityStub(
        TestActivities.class,
        new LocalActivityOptions.Builder()
            .setScheduleToCloseTimeout(Duration.ofSeconds(10))
            .build());
  }

  private static Throwable expectActivityFailure(Runnable activity) {
    try {
      activity.run();
    } catch (ActivityFailureException e) {
      return e.getCause();
    }
    throw new IllegalStateException("expected activity failure");
  }

  private static ApplicationException expectApplicationException(Runnable activity) {
    return expectApplicationException(expectActivityFailure(activity));
  }

  private static ApplicationException expectApplicationException(Throwable cause) {
    if (!(cause instanceof ApplicationException)) {
      throw new IllegalStateException("expected ApplicationException cause", cause);
    }
    return (ApplicationException) cause;
  }

  private static <T> T details(ApplicationException e, Class<T> type) {
    return CONVERTER.fromData(e.getDetailsBytes(CONVERTER), type, type);
  }

  private static void expectEquals(String what, Object expected, Object actual) {
    if (!Objects.equals(expected, actual)) {
      throw new IllegalStateException(
          "unexpected " + what + ": expected <" + expected + "> but was <" + actual + ">");
    }
  }

  @Test
  public void applicationException() {
    testRule.newWorkflowStub(ApplicationExceptionWorkflow.class).run();
  }

  @Test
  public void legacyException() {
    testRule.newWorkflowStub(LegacyExceptionWorkflow.class).run();
  }

  @Test
  public void fatalIsNotRetried() {
    testRule.newWorkflowStub(FatalWorkflow.class).run();
  }

  @Test
  public void nextRetryDelayOverridesRetryPolicy() {
    testRule.newWorkflowStub(NextRetryDelayWorkflow.class).run();
  }

  @Test
  public void childWorkflowApplicationException() {
    testRule.newWorkflowStub(ParentWorkflow.class).run();
  }

  @Test
  public void workflowApplicationException() {
    try {
      testRule.newWorkflowStub(FailingWorkflow.class).run();
      fail("expected workflow failure");
    } catch (WorkflowFailureException e) {
      assertTrue(e.getCause() instanceof ApplicationException);
      ApplicationException cause = (ApplicationException) e.getCause();
      assertEquals("WorkflowReason", cause.getReason());
      assertEquals("workflow details", details(cause, String.class));
    }
  }
}
