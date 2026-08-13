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
 * Thrown when a {@link ContextPropagator#runWithContext} implementation catches and suppresses an
 * exception thrown by the wrapped task instead of letting it propagate. This is a contract
 * violation of {@link ContextPropagator#runWithContext} and indicates a bug in the propagator
 * implementation, not a workflow or activity failure, so it is modeled as an {@link Error} rather
 * than a checked/unchecked exception.
 */
public final class ContextPropagatorSwallowedExceptionError extends Error {

  ContextPropagatorSwallowedExceptionError(
      List<ContextPropagator> appliedPropagators, Throwable swallowed) {
    super(
        "A ContextPropagator swallowed an exception thrown by the task it wraps instead of "
            + "propagating it. ContextPropagator#runWithContext must not catch and suppress "
            + "exceptions from the task it is given -- only wrap the call in try/finally, never "
            + "try/catch. One of these configured propagators must be fixed: "
            + appliedPropagators
                .stream()
                .map(p -> p.getClass().getName())
                .collect(Collectors.joining(", ")),
        swallowed);
  }
}
