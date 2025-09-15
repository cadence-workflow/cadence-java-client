package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PollForActivityTaskRequest {
  private String domain;
  private TaskList taskList;
  private String identity;
  private TaskListMetadata taskListMetadata;
}
