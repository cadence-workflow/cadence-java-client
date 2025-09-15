package com.uber.cadence.shadower;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowParams {
  private String domain;
  private String taskList;
  private String workflowQuery;
  private byte[] nextPageToken;
  private double samplingRate;
  private Mode shadowMode;
  private ExitCondition exitCondition;
  private int concurrency;
  private WorkflowResult lastRunResult;
}
