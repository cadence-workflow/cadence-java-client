package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class HistoryBranch {
  private String treeID;
  private String branchID;
  private List<HistoryBranchRange> ancestors;
}
