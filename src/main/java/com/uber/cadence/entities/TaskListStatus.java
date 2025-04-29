package com.uber.cadence.entities;

import lombok.Data;

@Data
public class TaskListStatus {
    private Long backlogCountHint;
    private Long readLevel;
    private Long ackLevel;
    private double ratePerSecond;
    private TaskIDBlock taskIDBlock;
}
