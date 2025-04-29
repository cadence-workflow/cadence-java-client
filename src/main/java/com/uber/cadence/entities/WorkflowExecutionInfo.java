package com.uber.cadence.entities;

import lombok.Data;
import java.util.Map;

@Data
public class WorkflowExecutionInfo {
    private WorkflowExecution execution;
    private WorkflowType type;
    private Long startTime;
    private Long closeTime;
    private WorkflowExecutionCloseStatus closeStatus;
    private Long historyLength;
    private String parentDomainName;
    private String parentDomainId;
    private WorkflowExecution parentExecution;
    private Long executionTime;
    private Memo memo;
    private SearchAttributes searchAttributes;
    private ResetPoints autoResetPoints;
    private String taskList;
    private Boolean isCron;
}
