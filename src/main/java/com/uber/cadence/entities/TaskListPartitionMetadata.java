package com.uber.cadence.entities;

import lombok.Data;

@Data
public class TaskListPartitionMetadata {
    private String key;
    private String ownerHostName;
}
