package com.uber.cadence.shadower;

public class Constants {
  public static final String LocalDomainName = "cadence-shadower";
  public static final String TaskList = "cadence-shadower-tl";
  public static final String WorkflowName = "cadence-shadow-workflow";
  public static final String ScanWorkflowActivityName = "scanWorkflowActivity";
  public static final String ReplayWorkflowActivityName = "replayWorkflowActivity";
  public static final String WorkflowIDSuffix = "-shadow-workflow";
  public static final String ErrNonRetryableType =
      "com.uber.cadence.internal.shadowing.NonRetryableException";
}
