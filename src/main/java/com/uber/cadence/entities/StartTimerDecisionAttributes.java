package com.uber.cadence.entities;

import lombok.Data;

@Data
public class StartTimerDecisionAttributes {
    private String timerId;
    private Long startToFireTimeoutSeconds;
}
