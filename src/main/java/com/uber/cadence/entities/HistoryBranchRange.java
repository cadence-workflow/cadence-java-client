package com.uber.cadence.entities;

import lombok.Data;

@Data
public class HistoryBranchRange {
    private Long beginNodeId;
    private Long endNodeId;
}
