package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DescribeTaskListResponse {
  private List<PollerInfo> pollers;
  private TaskListStatus taskListStatus;
}
