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

package com.uber.cadence.worker;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CheckReturnValue;
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.internal.sync.RegistryInternal;
import com.uber.cadence.workflow.Functions.Func;
import com.uber.cadence.workflow.Functions.Func1;
import com.uber.cadence.workflow.WorkflowMethod;
import java.util.Objects;

/**
 * Immutable set of workflow and activity implementations that a {@link Worker} hosts. Set it on a
 * worker through {@link Worker#setRegistry(Registry)}.
 *
 * <p>Registration methods do not modify the registry. Each returns a new registry with the
 * implementations of this registry and the registered ones:
 *
 * <pre><code>
 *   Registry registry =
 *       Registry.newInstance()
 *           .registerWorkflowImplementationTypes(MyWorkflowImpl.class)
 *           .registerActivitiesImplementations(new MyActivitiesImpl());
 *   worker.setRegistry(registry);
 * </code></pre>
 *
 * <p>Registering a workflow or activity type that the registry already has throws {@link
 * IllegalStateException}. Registries are thread-safe and can be shared between workers.
 */
@CheckReturnValue
public final class Registry {

  private static final Registry EMPTY = new Registry(RegistryInternal.EMPTY);

  private final RegistryInternal registry;

  private Registry(RegistryInternal registry) {
    this.registry = registry;
  }

  /** Returns an empty registry. */
  public static Registry newInstance() {
    return EMPTY;
  }

  /**
   * Returns a registry that also has the given workflow implementation classes. A workflow
   * implementation class must implement at least one interface with a method annotated with {@link
   * WorkflowMethod}. That method becomes a workflow type that this registry supports.
   *
   * @see Worker#registerWorkflowImplementationTypes(Class[])
   */
  public Registry registerWorkflowImplementationTypes(Class<?>... workflowImplementationClasses) {
    return registerWorkflowImplementationTypes(
        new WorkflowImplementationOptions.Builder().build(), workflowImplementationClasses);
  }

  /**
   * Returns a registry that also has the given workflow implementation classes with the given
   * options.
   *
   * @see Worker#registerWorkflowImplementationTypes(WorkflowImplementationOptions, Class[])
   */
  public Registry registerWorkflowImplementationTypes(
      WorkflowImplementationOptions options, Class<?>... workflowImplementationClasses) {
    return new Registry(
        registry.withWorkflowImplementationTypes(options, workflowImplementationClasses));
  }

  /**
   * Returns a registry that also has a factory to use when an instance of a workflow implementation
   * is created. !IMPORTANT to provide newly created instances, each time factory is applied.
   *
   * @see Worker#addWorkflowImplementationFactory(WorkflowImplementationOptions, Class, Func)
   */
  public <R> Registry addWorkflowImplementationFactory(
      WorkflowImplementationOptions options, Class<R> workflowInterface, Func<R> factory) {
    return new Registry(
        registry.withWorkflowImplementationFactory(options, workflowInterface, factory));
  }

  /**
   * Returns a registry that also has a factory to use when an instance of a workflow implementation
   * is created. The only valid use for this method is unit testing, specifically to instantiate
   * mocks that implement child workflows.
   *
   * @see Worker#addWorkflowImplementationFactory(Class, Func)
   */
  @VisibleForTesting
  public <R> Registry addWorkflowImplementationFactory(
      Class<R> workflowInterface, Func<R> factory) {
    return new Registry(registry.withWorkflowImplementationFactory(workflowInterface, factory));
  }

  /**
   * Returns a registry that also has the given activity implementation objects. Each method of the
   * interfaces an object implements becomes an activity type, named by {@link
   * ActivityMethod#name()} if present.
   *
   * @see Worker#registerActivitiesImplementations(Object...)
   */
  public Registry registerActivitiesImplementations(Object... activityImplementations) {
    return new Registry(registry.withActivityImplementations(activityImplementations));
  }

  /**
   * Returns a registry that also has a function as the implementation of the activity type {@code
   * activityType}. The argument and result are converted with the worker's {@link
   * com.uber.cadence.converter.DataConverter}. Workflows invoke the activity by name, for example
   * through {@link com.uber.cadence.workflow.Workflow#newUntypedActivityStub}. The function can use
   * {@link com.uber.cadence.activity.Activity} the same way as interface based implementations.
   *
   * @param activityType name of the activity type
   * @param argType type of the single activity argument
   * @param resultType type of the activity result, {@code void.class} or {@code Void.class} if the
   *     activity returns no result
   * @param activity function that implements the activity
   */
  public <A, R> Registry registerActivity(
      String activityType, Class<A> argType, Class<R> resultType, Func1<A, R> activity) {
    return new Registry(registry.withActivity(activityType, argType, resultType, activity));
  }

  /**
   * Returns a registry with the workflow and activity implementations of both registries. Throws
   * {@link IllegalStateException} if both registries have the same workflow or activity type.
   */
  public Registry registerAll(Registry other) {
    return new Registry(registry.withAll(Objects.requireNonNull(other).registry));
  }

  RegistryInternal getRegistryInternal() {
    return registry;
  }
}
