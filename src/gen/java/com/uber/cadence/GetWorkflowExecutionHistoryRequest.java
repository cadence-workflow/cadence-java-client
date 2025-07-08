package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GetWorkflowExecutionHistoryRequest {
  private String domain;
  private WorkflowExecution execution;
  private int maximumPageSize;
  private byte[] nextPageToken;
  private boolean waitForNewEvent;
  private HistoryEventFilterType HistoryEventFilterType;
  private boolean skipArchival;
}
