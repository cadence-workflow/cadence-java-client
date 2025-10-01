package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListTaskListPartitionsResponse {
  private List<TaskListPartitionMetadata> activityTaskListPartitions = new ArrayList<>();;
  private List<TaskListPartitionMetadata> decisionTaskListPartitions = new ArrayList<>();;
}
