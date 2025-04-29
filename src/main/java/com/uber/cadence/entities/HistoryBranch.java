package com.uber.cadence.entities;

import java.util.List;
import lombok.Data;

@Data
public class HistoryBranch {
    private String treeId;
    private String branchId;
    private List<HistoryBranchRange> ranges;
}
