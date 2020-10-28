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

package com.uber.cadence.activity;

import com.uber.cadence.workflow.Workflow;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the interface is an activity interface. Only interfaces annotated with this
 * annotation can be used as parameters to {@link Workflow#newActivityStub(Class)} methods.
 *
 * <p>Each method of the interface annotated with <code>ActivityInterface</code> including inherited
 * from interfaces is a separate activity. By default the name of an activity type is "short
 * interface name"_"method name".
 *
 * <p>Example:
 *
 * <pre><code>
 *  public interface A {
 *      a();
 *  }
 *
 * {@literal @}ActivityInterface
 *  public interface B extends A {
 *     b();
 *  }
 *
 * {@literal @}ActivityInterface
 *  public interface C extends B {
 *     c();
 *  }
 *
 *  public class CImpl implements C {
 *      public void a() {}
 *      public void b() {}
 *      public void c() {}
 *  }
 * </code></pre>
 *
 * When <code>CImpl</code> instance is registered with the {@link com.uber.cadence.worker.Worker} the
 * following activities are registered:
 *
 * <p>
 *
 * <ul>
 *   <li>B_a
 *   <li>B_b
 *   <li>C_c
 * </ul>
 *
 * Note that method <code>a()</code> is registered as "B_a" because interface <code>A</code> lacks
 * ActivityInterface annotation. The workflow code can call activities through stubs to <code>B
 * </code> and <code>C</code> interfaces. A call to crate stub to <code>A</code> interface will fail
 * as <code>A</code> is not annotated with ActivityInterface.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ActivityInterface {}
