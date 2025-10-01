package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GetCrossClusterTasksResponse {
  private Map<Integer, List<CrossClusterTaskRequest>> tasksByShard = new HashMap<>();;
  private Map<Integer, GetTaskFailedCause> failedCauseByShard = new HashMap<>();;
}
