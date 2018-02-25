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
package com.uber.cadence.workflow;

import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.MethodRetry;

/**
 * Options used to configure how an activity is invoked.
 */
public final class ActivityOptions {

    /**
     * Used to merge annotation and options. Options takes precedence.
     */
    static ActivityOptions merge(ActivityMethod a, MethodRetry r, ActivityOptions o) {
        return new ActivityOptions.Builder()
                .setScheduleToStartTimeoutSeconds(
                        merge(a.scheduleToCloseTimeoutSeconds(), o.getScheduleToCloseTimeoutSeconds()))
                .setScheduleToStartTimeoutSeconds(
                        merge(a.scheduleToStartTimeoutSeconds(), o.getScheduleToStartTimeoutSeconds()))
                .setStartToCloseTimeoutSeconds(
                        merge(a.startToCloseTimeoutSeconds(), o.getStartToCloseTimeoutSeconds()))
                .setHeartbeatTimeoutSeconds(
                        merge(a.heartbeatTimeoutSeconds(), o.getHeartbeatTimeoutSeconds()))
                .setTaskList(
                        o.getTaskList() != null ? o.getTaskList() : (a.taskList().isEmpty() ? null : a.taskList()))
                .setRetryOptions(
                        RetryOptions.merge(r, o.getRetryOptions()))
                .build();
    }

    public static final class Builder {

        private int heartbeatTimeoutSeconds;

        private int scheduleToCloseTimeoutSeconds;

        private int scheduleToStartTimeoutSeconds;

        private int startToCloseTimeoutSeconds;

        private String taskList;

        private RetryOptions retryOptions;

        public Builder() {
        }

        /**
         * Copy Builder fields from the options.
         */
        public Builder(ActivityOptions options) {
            this.scheduleToStartTimeoutSeconds = options.getScheduleToStartTimeoutSeconds();
            this.scheduleToCloseTimeoutSeconds = options.getScheduleToCloseTimeoutSeconds();
            this.heartbeatTimeoutSeconds = options.getHeartbeatTimeoutSeconds();
            this.startToCloseTimeoutSeconds = options.getStartToCloseTimeoutSeconds();
            this.taskList = options.taskList;
            this.retryOptions = options.retryOptions;
        }

        /**
         * Overall timeout workflow is willing to wait for activity to complete.
         * It includes time in a task list (use {@link #setScheduleToStartTimeoutSeconds(int)} to limit it)
         * plus activity execution time (use {@link #setStartToCloseTimeoutSeconds(int)} to limit it).
         * Either this option or both schedule to start and start to close are required.
         */
        public Builder setScheduleToCloseTimeoutSeconds(int scheduleToCloseTimeoutSeconds) {
            this.scheduleToCloseTimeoutSeconds = scheduleToCloseTimeoutSeconds;
            return this;
        }

        /**
         * Time activity can stay in task list before it is picked up by a worker.
         * If schedule to close is not provided then both this and start to close are required.
         */
        public Builder setScheduleToStartTimeoutSeconds(int scheduleToStartTimeoutSeconds) {
            this.scheduleToStartTimeoutSeconds = scheduleToStartTimeoutSeconds;
            return this;
        }

        /**
         * Maximum activity execution time after it was sent to a worker.
         * If schedule to close is not provided then both this and schedule to start are required.
         */
        public Builder setStartToCloseTimeoutSeconds(int startToCloseTimeoutSeconds) {
            this.startToCloseTimeoutSeconds = startToCloseTimeoutSeconds;
            return this;
        }

        /**
         * Heartbeat interval. Activity must heartbeat before this interval passes after a last heartbeat
         * or activity start.
         */
        public Builder setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
            this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
            return this;
        }

        /**
         * Task list to use when dispatching activity task to a worker. By default it is the same task list name
         * the workflow was started with.
         */
        public Builder setTaskList(String taskList) {
            this.taskList = taskList;
            return this;
        }

        /**
         * RetryOptions that define how activity is retried in case of failure. Default is null which is no reties.
         */
        public Builder setRetryOptions(RetryOptions retryOptions) {
            this.retryOptions = retryOptions;
            return this;
        }

        public ActivityOptions build() {
            return new ActivityOptions(heartbeatTimeoutSeconds, scheduleToCloseTimeoutSeconds,
                    scheduleToStartTimeoutSeconds, startToCloseTimeoutSeconds, taskList, retryOptions);
        }
    }

    private final int heartbeatTimeoutSeconds;

    private final int scheduleToCloseTimeoutSeconds;

    private final int scheduleToStartTimeoutSeconds;

    private final int startToCloseTimeoutSeconds;

    private final String taskList;

    private final RetryOptions retryOptions;

    private ActivityOptions(int heartbeatTimeoutSeconds, int scheduleToCloseTimeoutSeconds,
                            int scheduleToStartTimeoutSeconds, int startToCloseTimeoutSeconds, String taskList, RetryOptions retryOptions) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        this.scheduleToCloseTimeoutSeconds = scheduleToCloseTimeoutSeconds;
        if (scheduleToCloseTimeoutSeconds != 0) {
            if (scheduleToStartTimeoutSeconds == 0) {
                this.scheduleToStartTimeoutSeconds = scheduleToCloseTimeoutSeconds;
            } else {
                this.scheduleToStartTimeoutSeconds = scheduleToStartTimeoutSeconds;
            }
            if (startToCloseTimeoutSeconds == 0) {
                this.startToCloseTimeoutSeconds = scheduleToCloseTimeoutSeconds;
            } else {
                this.startToCloseTimeoutSeconds = startToCloseTimeoutSeconds;
            }
        } else {
            if (scheduleToStartTimeoutSeconds == 0 || startToCloseTimeoutSeconds == 0) {
                throw new IllegalStateException("Either ScheduleToClose or both ScheduleToStart and StarToClose " +
                        "timeouts are required: ");
            }
            this.scheduleToStartTimeoutSeconds = scheduleToStartTimeoutSeconds;
            this.startToCloseTimeoutSeconds = startToCloseTimeoutSeconds;
        }
        this.taskList = taskList;
        this.retryOptions = retryOptions;
    }

    public int getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }

    public int getScheduleToCloseTimeoutSeconds() {
        return scheduleToCloseTimeoutSeconds;
    }

    public int getScheduleToStartTimeoutSeconds() {
        return scheduleToStartTimeoutSeconds;
    }

    public int getStartToCloseTimeoutSeconds() {
        return startToCloseTimeoutSeconds;
    }

    public String getTaskList() {
        return taskList;
    }

    public RetryOptions getRetryOptions() {
        return retryOptions;
    }

    @Override
    public String toString() {
        return "ActivityOptions{" +
                "heartbeatTimeoutSeconds=" + heartbeatTimeoutSeconds +
                ", scheduleToCloseTimeoutSeconds=" + scheduleToCloseTimeoutSeconds +
                ", scheduleToStartTimeoutSeconds=" + scheduleToStartTimeoutSeconds +
                ", startToCloseTimeoutSeconds=" + startToCloseTimeoutSeconds +
                ", taskList='" + taskList + '\'' +
                retryOptions != null ? ", retryOptions=" + retryOptions : "" +
                '}';
    }

    private static int merge(int annotation, int options) {
        if (options == 0) {
            return annotation;
        } else {
            return options;
        }
    }
}
