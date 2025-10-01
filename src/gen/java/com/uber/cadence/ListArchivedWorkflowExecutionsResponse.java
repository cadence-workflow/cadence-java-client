package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListArchivedWorkflowExecutionsResponse {
  private List<WorkflowExecutionInfo> executions = new ArrayList<>();;
  private byte[] nextPageToken;
}
