package com.uber.cadence.entities;

import lombok.Data;

@Data
public class IsolationGroupPartition {
    private String name;
    private Integer percentage;
}
