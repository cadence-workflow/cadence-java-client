package com.uber.cadence.entities;


import lombok.Data;

@Data
public class ScheduleActivityTaskDecisionAttributes {
    private ActivityType activityType;
    private String activityId;
    private String domain;
    private TaskList taskList;
    private byte[] input;
    private Integer scheduleToCloseTimeoutSeconds;
    private Integer scheduleToStartTimeoutSeconds;
    private Integer startToCloseTimeoutSeconds;
    private Integer heartbeatTimeoutSeconds;
    private RetryPolicy retryPolicy;
    private Header header;
    private boolean requestLocalDispatch;
}
