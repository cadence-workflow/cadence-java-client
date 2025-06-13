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

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PollForDecisionTaskResponse {
  private byte[] taskToken;
  private WorkflowExecution workflowExecution;
  private WorkflowType workflowType;
  private long previousStartedEventId;
  private long startedEventId;
  private long attempt;
  private long backlogCountHint;
  private History history;
  private byte[] nextPageToken;
  private WorkflowQuery query;
  private TaskList WorkflowExecutionTaskList;
  private long scheduledTimestamp;
  private long startedTimestamp;
  private Map<String, WorkflowQuery> queries;
  private long nextEventId;
  private long totalHistoryBytes;
}
