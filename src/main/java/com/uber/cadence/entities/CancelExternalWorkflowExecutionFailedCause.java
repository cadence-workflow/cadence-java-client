/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.entities;

public enum CancelExternalWorkflowExecutionFailedCause {
  UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION,
  WORKFLOW_ALREADY_COMPLETED,
  WORKFLOW_ALREADY_CANCELED,
  WORKFLOW_ALREADY_TERMINATED,
  WORKFLOW_ALREADY_TIMED_OUT,
  WORKFLOW_ALREADY_FAILED,
  WORKFLOW_ALREADY_CONTINUED_AS_NEW
}
