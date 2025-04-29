package com.uber.cadence.entities;

import lombok.Data;
import java.util.List;

@Data
public class ListTaskListPartitionsResponse {
    private List<TaskListPartitionMetadata> activityTaskListPartitions;
    private List<TaskListPartitionMetadata> decisionTaskListPartitions;
}
