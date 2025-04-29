package com.uber.cadence.entities;

import lombok.Data;
import java.util.List;

@Data
public class DescribeWorkflowExecutionResponse {
    private WorkflowExecutionConfiguration executionConfiguration;
    private WorkflowExecutionInfo workflowExecutionInfo;
    private List<PendingActivityInfo> pendingActivities;
    private List<PendingChildExecutionInfo> pendingChildren;
    private PendingDecisionInfo pendingDecision;
}
