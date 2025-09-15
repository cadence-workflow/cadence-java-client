package com.uber.cadence.shadower;

import com.uber.cadence.WorkflowExecution;
import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScanWorkflowActivityResult {
  private List<WorkflowExecution> executions;
  private byte[] nextPageToken;
}
