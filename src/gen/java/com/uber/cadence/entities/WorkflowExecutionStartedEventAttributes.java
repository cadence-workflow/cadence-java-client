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
public class WorkflowExecutionStartedEventAttributes {
  private WorkflowType workflowType;
  private String parentWorkflowDomain;
  private WorkflowExecution parentWorkflowExecution;
  private long parentInitiatedEventId;
  private TaskList taskList;
  private byte[] input;
  private int executionStartToCloseTimeoutSeconds;
  private int taskStartToCloseTimeoutSeconds;
  private String continuedExecutionRunId;
  private ContinueAsNewInitiator initiator;
  private String continuedFailureReason;
  private byte[] continuedFailureDetails;
  private byte[] lastCompletionResult;
  private String originalExecutionRunId;
  private String identity;
  private String firstExecutionRunId;
  private long firstScheduledTimeNano;
  private RetryPolicy retryPolicy;
  private int attempt;
  private long expirationTimestamp;
  private String cronSchedule;
  private int firstDecisionTaskBackoffSeconds;
  private Memo memo;
  private SearchAttributes searchAttributes;
  private ResetPoints prevAutoResetPoints;
  private Header header;
  private Map<String, String> partitionConfig;
  private String requestId;
}
