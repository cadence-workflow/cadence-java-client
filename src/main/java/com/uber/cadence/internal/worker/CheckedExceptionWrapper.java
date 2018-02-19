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
package com.uber.cadence.internal.worker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Do not reference directly by the application level code.
 * Use {@link com.uber.cadence.workflow.Workflow#throwWrapped(Throwable)} inside a workflow code and
 * {@link com.uber.cadence.activity.Activity#throwWrapped(Throwable)} inside an activity code instead.
 */
public final class CheckedExceptionWrapper extends RuntimeException {

    private final static Field causeField;

    static {
        try {
            causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("unexpected", e);
        }
    }

    /**
     * Throws CheckedExceptionWrapper if e is checked exception.
     * If there is a need to return a checked exception from an activity or workflow implementation
     * throwWrapped it using this method. The library code will unwrap it automatically when propagating exception
     * to the caller.
     * <p>
     * Throws original exception if e is {@link RuntimeException} or {@link Error}.
     * Never returns. But return type is not empty to be able to use it as:
     * <pre>
     * try {
     *     return someCall();
     * } catch (Exception e) {
     *     throw CheckedExceptionWrapper.throwWrapped(e);
     * }
     * </pre>
     * If throwWrapped returned void it wouldn't be possible to write <code>throw CheckedExcptionWrapper.throwWrapped</code>
     * and compiler would complain about missing return.
     *
     * @return never returns as always throws.
     */
    public static RuntimeException throwWrapped(Throwable e) {
        if (e instanceof Error) {
            throw (Error) e;
        }
        if (e instanceof InvocationTargetException) {
            throw throwWrapped(e.getCause());
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new CheckedExceptionWrapper((Exception) e);
    }

    /**
     * Removes CheckedException wrapper from the whole chain of Exceptions.
     * Assumes that wrapper always has a cause which cannot be a wrapper.
     * \
     */
    public static Throwable unwrap(Throwable e) {
        // Assuming that wrapper always has a cause which cannot be a wrapper.
        Throwable head = e;
        if (head instanceof CheckedExceptionWrapper) {
            head = head.getCause();
        }
        Throwable tail = head;
        Throwable current = tail.getCause();
        while (current != null) {
            if (current instanceof CheckedExceptionWrapper) {
                current = current.getCause();
//                tail.initCause(current);
                try {
                    causeField.set(tail, current);
                } catch (IllegalAccessException e1) {
                    throw new RuntimeException(e1);
                }
            }
            tail = current;
            current = tail.getCause();
        }
        return head;
    }

    private CheckedExceptionWrapper(Exception e) {
        super(e);
    }
}
