package com.uber.cadence.entities;

import lombok.Data;

@Data
public class ActivityLocalDispatchInfo {
    private String activityId;
    private Long scheduledTimestamp;
    private Long startedTimestamp;
    private Long scheduledTimestampOfThisAttempt;
    private byte[] taskToken;

}
