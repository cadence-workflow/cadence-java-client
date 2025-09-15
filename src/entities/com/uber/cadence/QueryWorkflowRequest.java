package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class QueryWorkflowRequest {
  private String domain;
  private WorkflowExecution execution;
  private WorkflowQuery query;
  private QueryRejectCondition queryRejectCondition;
  private QueryConsistencyLevel queryConsistencyLevel;
}
