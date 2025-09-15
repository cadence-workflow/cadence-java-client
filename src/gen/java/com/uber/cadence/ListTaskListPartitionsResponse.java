package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListTaskListPartitionsResponse {
  private List<TaskListPartitionMetadata> activityTaskListPartitions;
  private List<TaskListPartitionMetadata> decisionTaskListPartitions;
}
