package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespondDecisionTaskCompletedRequest {
  private byte[] taskToken;
  private List<Decision> decisions;
  private byte[] executionContext;
  private String identity;
  private StickyExecutionAttributes stickyAttributes;
  private boolean returnNewDecisionTask;
  private boolean forceCreateNewDecisionTask;
  private String binaryChecksum;
  private Map<String, WorkflowQueryResult> queryResults;
}
