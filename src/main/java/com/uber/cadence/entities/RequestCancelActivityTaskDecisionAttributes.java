package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RequestCancelActivityTaskDecisionAttributes {
    private String activityId;
}
