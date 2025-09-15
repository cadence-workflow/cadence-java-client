package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespondCrossClusterTasksCompletedRequest {
  private int shardID;
  private String targetCluster;
  private List<CrossClusterTaskResponse> taskResponses;
  private boolean fetchNewTasks;
}
