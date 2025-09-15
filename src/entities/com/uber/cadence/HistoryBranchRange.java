package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class HistoryBranchRange {
  private String branchID;
  private long beginNodeID;
  private long endNodeID;
}
