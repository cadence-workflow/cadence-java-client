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

package com.uber.cadence.internal.sync;

import static com.uber.cadence.worker.NonDeterministicWorkflowPolicy.FailWorkflow;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.common.MethodRetry;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.worker.WorkflowImplementationOptions;
import com.uber.cadence.workflow.Functions;
import com.uber.cadence.workflow.QueryMethod;
import com.uber.cadence.workflow.SignalMethod;
import com.uber.cadence.workflow.WorkflowMethod;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable set of workflow and activity implementations keyed by type name. Every operation that
 * adds or removes implementations returns a new instance.
 *
 * <p>Registering a type that the registry already has is rejected.
 */
public final class RegistryInternal {

  public static final RegistryInternal EMPTY =
      new RegistryInternal(ImmutableMap.of(), ImmutableMap.of());

  private final ImmutableMap<String, WorkflowRegistration> workflows;
  private final ImmutableMap<String, ActivityRegistration> activities;

  private RegistryInternal(
      ImmutableMap<String, WorkflowRegistration> workflows,
      ImmutableMap<String, ActivityRegistration> activities) {
    this.workflows = workflows;
    this.activities = activities;
  }

  /** Returns a registry with the activities of this registry and no workflows. */
  public RegistryInternal withoutWorkflows() {
    return new RegistryInternal(ImmutableMap.of(), activities);
  }

  /** Returns a registry with the workflows of this registry and no activities. */
  public RegistryInternal withoutActivities() {
    return new RegistryInternal(workflows, ImmutableMap.of());
  }

  public RegistryInternal withWorkflowImplementationTypes(
      WorkflowImplementationOptions options, Class<?>... workflowImplementationClasses) {
    Map<String, WorkflowRegistration> added = new LinkedHashMap<>();
    for (Class<?> type : workflowImplementationClasses) {
      collectWorkflowImplementationType(options, type, null, added);
    }
    return new RegistryInternal(merge(workflows, added, "workflow"), activities);
  }

  public <R> RegistryInternal withWorkflowImplementationFactory(
      Class<R> workflowInterface, Functions.Func<R> factory) {
    WorkflowImplementationOptions unitTestingOptions =
        new WorkflowImplementationOptions.Builder()
            .setNonDeterministicWorkflowPolicy(FailWorkflow)
            .build();
    return withWorkflowImplementationFactory(unitTestingOptions, workflowInterface, factory);
  }

  public <R> RegistryInternal withWorkflowImplementationFactory(
      WorkflowImplementationOptions options,
      Class<R> workflowInterface,
      Functions.Func<R> factory) {
    Objects.requireNonNull(factory);
    Map<String, WorkflowRegistration> added = new LinkedHashMap<>();
    collectWorkflowImplementationType(options, workflowInterface, factory, added);
    return new RegistryInternal(merge(workflows, added, "workflow"), activities);
  }

  public RegistryInternal withActivityImplementations(Object... activityImplementations) {
    Map<String, ActivityRegistration> added = new LinkedHashMap<>();
    for (Object activity : activityImplementations) {
      collectActivityImplementation(activity, added);
    }
    return new RegistryInternal(workflows, merge(activities, added, "activity"));
  }

  @SuppressWarnings("unchecked")
  public <A, R> RegistryInternal withActivity(
      String activityType, Class<A> argType, Class<R> resultType, Functions.Func1<A, R> activity) {
    Objects.requireNonNull(activityType, "activityType");
    if (activityType.isEmpty()) {
      throw new IllegalArgumentException("Activity type must not be empty");
    }
    Objects.requireNonNull(argType, "argType");
    Objects.requireNonNull(resultType, "resultType");
    Objects.requireNonNull(activity, "activity");
    boolean returnsVoid = resultType == Void.TYPE || resultType == Void.class;
    ActivityRegistration registration =
        new ActivityRegistration(
            new Type[] {argType}, returnsVoid, (args) -> activity.apply((A) args[0]));
    return new RegistryInternal(
        workflows,
        merge(activities, Collections.singletonMap(activityType, registration), "activity"));
  }

  /** Returns a registry with the implementations of both registries. */
  public RegistryInternal withAll(RegistryInternal other) {
    return new RegistryInternal(
        merge(workflows, other.workflows, "workflow"),
        merge(activities, other.activities, "activity"));
  }

  WorkflowRegistration getWorkflow(String workflowType) {
    return workflows.get(workflowType);
  }

  ActivityRegistration getActivity(String activityType) {
    return activities.get(activityType);
  }

  Set<String> getWorkflowTypes() {
    return workflows.keySet();
  }

  Set<String> getActivityTypes() {
    return activities.keySet();
  }

  boolean hasWorkflows() {
    return !workflows.isEmpty();
  }

  boolean hasActivities() {
    return !activities.isEmpty();
  }

  private static <T> ImmutableMap<String, T> merge(
      ImmutableMap<String, T> current, Map<String, T> added, String kind) {
    for (String type : added.keySet()) {
      if (current.containsKey(type)) {
        throw new IllegalStateException(
            type + " " + kind + " type is already registered with the worker");
      }
    }
    return ImmutableMap.<String, T>builder().putAll(current).putAll(added).build();
  }

  private static void collectWorkflowImplementationType(
      WorkflowImplementationOptions options,
      Class<?> workflowImplementationClass,
      Functions.Func<?> instanceFactory,
      Map<String, WorkflowRegistration> added) {
    TypeToken<?>.TypeSet interfaces =
        TypeToken.of(workflowImplementationClass).getTypes().interfaces();
    if (interfaces.isEmpty()) {
      throw new IllegalArgumentException("Workflow must implement at least one interface");
    }
    boolean hasWorkflowMethod = false;
    for (TypeToken<?> i : interfaces) {
      Map<String, Method> signalHandlers = new HashMap<>();
      Map<String, Method> workflowMethods = new LinkedHashMap<>();
      for (Method method : i.getRawType().getMethods()) {
        Optional<WorkflowMethod> workflowMethod =
            InternalUtils.getWorkflowAnnotation(method, WorkflowMethod.class);
        Optional<QueryMethod> queryMethod =
            InternalUtils.getWorkflowAnnotation(method, QueryMethod.class);
        Optional<SignalMethod> signalMethod =
            InternalUtils.getWorkflowAnnotation(method, SignalMethod.class);
        if (workflowMethod.isPresent()) {
          String workflowName = workflowMethod.get().name();
          if (workflowName.isEmpty()) {
            workflowName = InternalUtils.getSimpleName(method);
          }
          if (added.containsKey(workflowName) || workflowMethods.containsKey(workflowName)) {
            throw new IllegalStateException(
                workflowName + " workflow type is already registered with the worker");
          }
          workflowMethods.put(workflowName, method);
          hasWorkflowMethod = true;
        }
        if (signalMethod.isPresent()) {
          if (method.getReturnType() != Void.TYPE) {
            throw new IllegalArgumentException(
                "Method annotated with @SignalMethod " + "must have void return type: " + method);
          }
          String signalName = signalMethod.get().name();
          if (signalName.isEmpty()) {
            signalName = InternalUtils.getSimpleName(method);
          }
          signalHandlers.put(signalName, method);
        }
        if (queryMethod.isPresent()) {
          if (method.getReturnType() == Void.TYPE) {
            throw new IllegalArgumentException(
                "Method annotated with @QueryMethod " + "cannot have void return type: " + method);
          }
        }
      }
      Map<String, Method> interfaceSignalHandlers = Collections.unmodifiableMap(signalHandlers);
      for (Map.Entry<String, Method> e : workflowMethods.entrySet()) {
        added.put(
            e.getKey(),
            new WorkflowRegistration(
                e.getValue(),
                workflowImplementationClass,
                interfaceSignalHandlers,
                instanceFactory,
                options));
      }
    }
    if (!hasWorkflowMethod) {
      throw new IllegalArgumentException(
          "Workflow implementation doesn't implement any interface "
              + "with a workflow method annotated with @WorkflowMethod: "
              + workflowImplementationClass);
    }
  }

  private static void collectActivityImplementation(
      Object activity, Map<String, ActivityRegistration> added) {
    if (activity instanceof Class) {
      throw new IllegalArgumentException("Activity object instance expected, not the class");
    }
    Class<?> cls = activity.getClass();
    for (Method method : cls.getMethods()) {
      if (method.getAnnotation(ActivityMethod.class) != null) {
        throw new IllegalArgumentException(
            "Found @ActivityMethod annotation on \""
                + method
                + "\" This annotation can be used only on the interface method it implements.");
      }
      if (method.getAnnotation(MethodRetry.class) != null) {
        throw new IllegalArgumentException(
            "Found @MethodRetry annotation on \""
                + method
                + "\" This annotation can be used only on the interface method it implements.");
      }
    }
    TypeToken<?>.TypeSet interfaces = TypeToken.of(cls).getTypes().interfaces();
    if (interfaces.isEmpty()) {
      throw new IllegalArgumentException("Activity must implement at least one interface");
    }
    List<Method> methods = new ArrayList<>();
    for (TypeToken<?> i : interfaces) {
      if (i.getType().getTypeName().startsWith("org.mockito")) {
        continue;
      }
      Collections.addAll(methods, i.getRawType().getMethods());
    }
    for (Method method : methods) {
      ActivityMethod annotation = method.getAnnotation(ActivityMethod.class);
      String activityType;
      if (annotation != null && !annotation.name().isEmpty()) {
        activityType = annotation.name();
      } else {
        activityType = InternalUtils.getSimpleName(method);
      }
      if (added.containsKey(activityType)) {
        throw new IllegalStateException(
            activityType + " activity type is already registered with the worker");
      }
      added.put(
          activityType,
          new ActivityRegistration(
              method.getGenericParameterTypes(),
              method.getReturnType() == Void.TYPE,
              (args) -> {
                try {
                  return method.invoke(activity, args);
                } catch (InvocationTargetException e) {
                  throw e.getTargetException();
                }
              }));
    }
  }

  @Override
  public String toString() {
    return "RegistryInternal{"
        + "workflowTypes="
        + getWorkflowTypes()
        + ", activityTypes="
        + getActivityTypes()
        + '}';
  }
}
