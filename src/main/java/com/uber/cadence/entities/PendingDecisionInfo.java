package com.uber.cadence.entities;

import lombok.Data;

@Data
public class PendingDecisionInfo {
    private PendingDecisionState state;
    private Long scheduledTimestamp;
    private Long startedTimestamp;
    private Integer attempt;
    private Long originalScheduledTimestamp;
}
