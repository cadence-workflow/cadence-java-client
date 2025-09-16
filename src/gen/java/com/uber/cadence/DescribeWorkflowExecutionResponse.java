package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DescribeWorkflowExecutionResponse {
  private WorkflowExecutionConfiguration executionConfiguration;
  private WorkflowExecutionInfo workflowExecutionInfo;
  private List<PendingActivityInfo> pendingActivities;
  private List<PendingChildExecutionInfo> pendingChildren;
  private PendingDecisionInfo pendingDecision;
}
