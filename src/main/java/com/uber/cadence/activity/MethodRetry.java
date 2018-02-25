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
package com.uber.cadence.activity;

import com.uber.cadence.workflow.Functions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates retry policy for a workflow or activity method.
 * This annotation applies only to activity or workflow interface methods.
 * Not required. When not used either retries don't happen or they are
 * configured through correspondent options.
 * If {@link com.uber.cadence.workflow.RetryOptions} are present on {@link com.uber.cadence.workflow.ActivityOptions}
 * the fields that are not default take precedence over parameters of this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodRetry {

    /**
     * Interval of the first retry. If coefficient is 1.0 then it is used for all retries.
     * Required!
     */
    long initialIntervalSeconds();

    /**
     * Maximum time to retry. Null means forever.
     * When exceeded the retries stop even if maximum retries is not reached yet.
     */
    long expirationSeconds() default Integer.MAX_VALUE;

    /**
     * Coefficient used to calculate the next retry interval.
     * The next retry interval is previous interval multiplied by this coefficient.
     * Must be 1 or larger.
     */
    double backoffCoefficient() default 2.0;

    /**
     * Maximum number of attempts. When exceeded the retries stop even if not expired yet.
     * Must be 1 or bigger.
     */
    int maximumAttempts() default Integer.MAX_VALUE;

    /**
     * Minimum number of retries. Even if expired will retry until this number is reached.
     * Must be 1 or bigger.
     */
    int minimumAttempts() default 0;

    /**
     * Maximum interval between retries. Exponential backoff leads to interval increase.
     * This value is the cap of the increase.
     */
    long maximumIntervalSeconds() default 0;

    /**
     * All filters should return true if exception should be retried.
     * {@link Error} and {@link java.util.concurrent.CancellationException} are never retried and
     * are not even passed to the filters.
     */
    Class<? extends Functions.Func1<Exception, Boolean>>[] exceptionFilters() default {};
}
