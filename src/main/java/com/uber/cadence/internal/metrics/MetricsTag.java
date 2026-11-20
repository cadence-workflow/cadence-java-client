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

package com.uber.cadence.internal.metrics;

public class MetricsTag {
  public static final String ACTIVITY_TYPE = "ActivityType";
  public static final String DOMAIN = "Domain";
  public static final String TASK_LIST = "TaskList";
  public static final String WORKFLOW_TYPE = "WorkflowType";
  public static final String REQUEST_TYPE = "RequestType";
  public static final String VERSION = "Version";
  public static final String CAUSE = "Cause";
  public static final String VERSION_MISMATCH_TYPE = "VersionMismatchType";
}
