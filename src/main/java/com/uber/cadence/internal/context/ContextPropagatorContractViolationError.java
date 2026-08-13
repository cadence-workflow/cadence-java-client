/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.uber.cadence.internal.context;

import com.uber.cadence.context.ContextPropagator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when a {@link ContextPropagator#runWithContext} implementation violates its contract:
 * either by catching and suppressing an exception thrown by the wrapped task instead of letting it
 * propagate, or by invoking the task a number of times other than exactly once (e.g. retrying it
 * after a failure, or never invoking it at all). This indicates a bug in the propagator
 * implementation, not a workflow or activity failure, so it is modeled as an {@link Error} rather
 * than a checked/unchecked exception.
 */
public final class ContextPropagatorContractViolationError extends Error {

  private ContextPropagatorContractViolationError(String message, Throwable cause) {
    super(message, cause);
  }

  static ContextPropagatorContractViolationError swallowedException(
      List<ContextPropagator> appliedPropagators, Throwable swallowed) {
    return new ContextPropagatorContractViolationError(
        "A ContextPropagator swallowed an exception thrown by the task it wraps instead of "
            + "propagating it. ContextPropagator#runWithContext must not catch and suppress "
            + "exceptions from the task it is given -- only wrap the call in try/finally, never "
            + "try/catch. One of these configured propagators must be fixed: "
            + propagatorNames(appliedPropagators),
        swallowed);
  }

  static ContextPropagatorContractViolationError unexpectedInvocationCount(
      List<ContextPropagator> appliedPropagators, int invocationCount) {
    return new ContextPropagatorContractViolationError(
        "A ContextPropagator invoked the task it wraps "
            + invocationCount
            + " time(s) instead of exactly once. ContextPropagator#runWithContext must call "
            + "task.run() exactly once -- it must not skip the call, and it must not retry the "
            + "task after catching an exception from it. One of these configured propagators "
            + "must be fixed: "
            + propagatorNames(appliedPropagators),
        null);
  }

  private static String propagatorNames(List<ContextPropagator> propagators) {
    return propagators.stream().map(p -> p.getClass().getName()).collect(Collectors.joining(", "));
  }
}
