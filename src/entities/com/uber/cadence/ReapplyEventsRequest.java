package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ReapplyEventsRequest {
  private String domainName;
  private WorkflowExecution workflowExecution;
  private DataBlob events;
}
