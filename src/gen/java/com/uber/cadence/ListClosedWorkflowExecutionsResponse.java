package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListClosedWorkflowExecutionsResponse {
  private List<WorkflowExecutionInfo> executions = new ArrayList<>();;
  private byte[] nextPageToken;
}
