package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowExecutionInfo {
  private WorkflowExecution execution;
  private WorkflowType type;
  private long startTime;
  private long closeTime;
  private WorkflowExecutionCloseStatus closeStatus;
  private long historyLength;
  private String parentDomainId;
  private String parentDomainName;
  private long parentInitatedId;
  private WorkflowExecution parentExecution;
  private long executionTime;
  private Memo memo;
  private SearchAttributes searchAttributes;
  private ResetPoints autoResetPoints;
  private String taskList;
  private boolean isCron;
  private long updateTime;
  private Map<String, String> partitionConfig = new HashMap<>();;
}
