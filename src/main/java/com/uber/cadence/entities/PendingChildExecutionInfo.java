package com.uber.cadence.entities;

import lombok.Data;

@Data
public class PendingChildExecutionInfo {
    private String domain;
    private String workflowID;
    private String runID;
    private String workflowTypName;
    private Long initiatedID;
    private ParentClosePolicy parentClosePolicy;
}
