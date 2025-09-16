package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CrossClusterRecordChildWorkflowExecutionCompleteRequestAttributes {
  private String targetDomainID;
  private String targetWorkflowID;
  private String targetRunID;
  private long initiatedEventID;
  private HistoryEvent completionEvent;
}
