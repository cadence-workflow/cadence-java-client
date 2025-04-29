package com.uber.cadence.entities;

import java.util.List;
import lombok.Data;

@Data
public class VersionHistory {
    private byte[] branchToken;
    private List<VersionHistoryItem> items;
}
