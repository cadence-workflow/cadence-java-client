package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CrossClusterSignalExecutionRequestAttributes {
  private String targetDomainID;
  private String targetWorkflowID;
  private String targetRunID;
  private String requestID;
  private long initiatedEventID;
  private boolean childWorkflowOnly;
  private String signalName;
  private byte[] signalInput;
  private byte[] control;
}
