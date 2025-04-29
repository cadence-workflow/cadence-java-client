package com.uber.cadence.entities;

import java.util.List;
import lombok.Data;

@Data
public class FailoverInfo {
    private Long failoverVersion;
    private Long failoverStartTimestamp;
    private Long failoverExpireTimestamp;
    private Integer completedShardCount;
    private List<Integer> pendingShards;
}
