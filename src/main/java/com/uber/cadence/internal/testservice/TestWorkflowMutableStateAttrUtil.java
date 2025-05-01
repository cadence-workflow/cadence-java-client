/*
 *
 *  *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  *
 *  *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  *  use this file except in compliance with the License. A copy of the License is
 *  *  located at
 *  *
 *  *  http://aws.amazon.com/apache2.0
 *  *
 *  *  or in the "license" file accompanying this file. This file is distributed on
 *  *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  *  express or implied. See the License for the specific language governing
 *  *  permissions and limitations under the License.
 *
 */

package com.uber.cadence.internal.testservice;

import com.uber.cadence.api.v1.*;
import com.uber.cadence.serviceclient.exceptions.BadRequestException;

public class TestWorkflowMutableStateAttrUtil {

  static void validateScheduleActivityTask(ScheduleActivityTaskDecisionAttributes a)
      throws BadRequestException {
    if (a == null) {
      throw new BadRequestException(
          "ScheduleActivityTaskDecisionAttributes is not set on decision.");
    }

    if (!a.hasTaskList() || a.getTaskList().getName().isEmpty()) {
      throw new BadRequestException("TaskList is not set on decision.");
    }
    if (a.getActivityId().isEmpty() || a.getActivityId().isEmpty()) {
      throw new BadRequestException("ActivityId is not set on decision.");
    }
    if (!a.hasActivityType() || a.getActivityType().getName().isEmpty()) {
      throw new BadRequestException("ActivityType is not set on decision.");
    }
    if (a.getStartToCloseTimeout().getSeconds() <= 0) {
      throw new BadRequestException("A valid StartToCloseTimeoutSeconds is not set on decision.");
    }
    if (a.getScheduleToStartTimeout().getSeconds() <= 0) {
      throw new BadRequestException(
          "A valid ScheduleToStartTimeoutSeconds is not set on decision.");
    }
    if (a.getScheduleToCloseTimeout().getSeconds() <= 0) {
      throw new BadRequestException(
          "A valid ScheduleToCloseTimeoutSeconds is not set on decision.");
    }
    if (a.getHeartbeatTimeout().getSeconds() < 0) {
      throw new BadRequestException("Ac valid HeartbeatTimeoutSeconds is not set on decision.");
    }
  }

  static void validateStartChildExecutionAttributes(StartChildWorkflowExecutionDecisionAttributes a)
      throws BadRequestException {
    if (a == null) {
      throw new BadRequestException(
          "StartChildWorkflowExecutionDecisionAttributes is not set on decision.");
    }

    if (a.getWorkflowId().isEmpty()) {
      throw new BadRequestException("Required field WorkflowID is not set on decision.");
    }

    if (a.getWorkflowType() == null || a.getWorkflowType().getName().isEmpty()) {
      throw new BadRequestException("Required field WorkflowType is not set on decision.");
    }

    RetryPolicy retryPolicy = a.getRetryPolicy();
    if (retryPolicy != null) {
      RetryState.validateRetryPolicy(retryPolicy);
    }
  }

  public static StartChildWorkflowExecutionDecisionAttributes
      inheritUnsetPropertiesFromParentWorkflow(
          StartWorkflowExecutionRequest startRequest,
          StartChildWorkflowExecutionDecisionAttributes a) {
    StartChildWorkflowExecutionDecisionAttributes.Builder res = a.toBuilder();
    // Inherit tasklist from parent workflow execution if not provided on decision
    if (!a.hasTaskList() || a.getTaskList().getName().isEmpty()) {
      res.setTaskList(startRequest.getTaskList());
    }

    // Inherit workflow timeout from parent workflow execution if not provided on decision
    if (!a.hasExecutionStartToCloseTimeout()) {
      res.setExecutionStartToCloseTimeout(startRequest.getExecutionStartToCloseTimeout());
    }

    // Inherit decision task timeout from parent workflow execution if not provided on decision
    if (!a.hasTaskStartToCloseTimeout()) {
      res.setTaskStartToCloseTimeout(startRequest.getTaskStartToCloseTimeout());
    }
    return res.build();
  }
}
