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

import static com.uber.cadence.internal.common.InternalUtils.getValueOrDefault;

import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.workflow.ContinueAsNewOptions;
import com.uber.cadence.workflow.WorkflowInterceptor;
import com.uber.cadence.workflow.WorkflowMethod;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Optional;

class ContinueAsNewWorkflowInvocationHandler implements InvocationHandler {

  private final ContinueAsNewOptions options;
  private final WorkflowInterceptor decisionContext;

  ContinueAsNewWorkflowInvocationHandler(
      ContinueAsNewOptions options, WorkflowInterceptor decisionContext) {
    this.options = options == null ? new ContinueAsNewOptions.Builder().build() : options;
    this.decisionContext = decisionContext;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    Optional<WorkflowMethod> workflowMethod =
        InternalUtils.getWorkflowAnnotation(method, WorkflowMethod.class);
    if (!workflowMethod.isPresent()) {
      throw new IllegalStateException(
          "ContinueAsNew Stub supports only calls to methods annotated with @WorkflowMethod");
    }
    String workflowType = InternalUtils.getWorkflowType(method, workflowMethod.get());
    WorkflowInternal.continueAsNew(
        Optional.of(workflowType), Optional.of(options), args, decisionContext);
    return getValueOrDefault(null, method.getReturnType());
  }
}
