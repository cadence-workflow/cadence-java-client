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

import com.uber.cadence.worker.WorkflowImplementationOptions;
import com.uber.cadence.workflow.Functions.Func;
import java.lang.reflect.Method;
import java.util.Map;

/** A workflow implementation registered under a workflow type name. */
final class WorkflowRegistration {

  private final Method workflowMethod;
  private final Class<?> implementationClass;
  private final Map<String, Method> signalHandlers;
  private final Func<?> instanceFactory;
  private final WorkflowImplementationOptions options;

  /**
   * @param instanceFactory factory that creates workflow instances, or null to instantiate {@code
   *     implementationClass} through its no-argument constructor.
   */
  WorkflowRegistration(
      Method workflowMethod,
      Class<?> implementationClass,
      Map<String, Method> signalHandlers,
      Func<?> instanceFactory,
      WorkflowImplementationOptions options) {
    this.workflowMethod = workflowMethod;
    this.implementationClass = implementationClass;
    this.signalHandlers = signalHandlers;
    this.instanceFactory = instanceFactory;
    this.options = options;
  }

  Method getWorkflowMethod() {
    return workflowMethod;
  }

  Class<?> getImplementationClass() {
    return implementationClass;
  }

  Map<String, Method> getSignalHandlers() {
    return signalHandlers;
  }

  Func<?> getInstanceFactory() {
    return instanceFactory;
  }

  WorkflowImplementationOptions getOptions() {
    return options;
  }
}
