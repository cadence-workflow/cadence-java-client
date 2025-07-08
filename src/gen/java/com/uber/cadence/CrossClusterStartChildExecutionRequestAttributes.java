package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CrossClusterStartChildExecutionRequestAttributes {
  private String targetDomainID;
  private String requestID;
  private long initiatedEventID;
  private StartChildWorkflowExecutionInitiatedEventAttributes initiatedEventAttributes;
  private String targetRunID;
  private Map<String, String> partitionConfig;
}
