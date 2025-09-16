package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DescribeHistoryHostRequest {
  private String hostAddress;
  private int shardIdForHost;
  private WorkflowExecution executionForHost;
}
