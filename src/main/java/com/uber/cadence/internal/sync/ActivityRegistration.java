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

import java.lang.reflect.Type;

/** An activity implementation registered under an activity type name. */
final class ActivityRegistration {

  /** Invokes the activity implementation with already deserialized arguments. */
  interface Invoker {
    Object invoke(Object[] args) throws Throwable;
  }

  private final Type[] parameterTypes;
  private final boolean returnsVoid;
  private final Invoker invoker;

  ActivityRegistration(Type[] parameterTypes, boolean returnsVoid, Invoker invoker) {
    this.parameterTypes = parameterTypes;
    this.returnsVoid = returnsVoid;
    this.invoker = invoker;
  }

  Type[] getParameterTypes() {
    return parameterTypes;
  }

  boolean isReturnsVoid() {
    return returnsVoid;
  }

  Object invoke(Object[] args) throws Throwable {
    return invoker.invoke(args);
  }
}
