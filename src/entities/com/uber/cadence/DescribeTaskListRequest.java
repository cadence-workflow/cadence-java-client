package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DescribeTaskListRequest {
  private String domain;
  private TaskList taskList;
  private TaskListType taskListType;
  private boolean includeTaskListStatus;
}
