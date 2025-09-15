package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListWorkflowExecutionsResponse {
  private List<WorkflowExecutionInfo> executions;
  private byte[] nextPageToken;
}
