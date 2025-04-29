package com.uber.cadence.entities;

import lombok.Data;

@Data
public class TaskIDBlock {
    private Long startID;
    private Long endID;
}
