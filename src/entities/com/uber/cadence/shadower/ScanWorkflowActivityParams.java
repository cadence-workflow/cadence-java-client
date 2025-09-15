package com.uber.cadence.shadower;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScanWorkflowActivityParams {
  private String domain;
  private String workflowQuery;
  private byte[] nextPageToken;
  private int pageSize;
  private double samplingRate;
}
