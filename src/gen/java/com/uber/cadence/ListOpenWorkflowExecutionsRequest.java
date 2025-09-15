package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListOpenWorkflowExecutionsRequest {
  private String domain;
  private int maximumPageSize;
  private byte[] nextPageToken;
  private StartTimeFilter StartTimeFilter;
  private WorkflowExecutionFilter executionFilter;
  private WorkflowTypeFilter typeFilter;
}
