package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PendingChildExecutionInfo {
  private String domain;
  private String workflowID;
  private String runID;
  private String workflowTypName;
  private long initiatedID;
  private ParentClosePolicy parentClosePolicy;
}
