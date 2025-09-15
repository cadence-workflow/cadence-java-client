package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PollForDecisionTaskRequest {
  private String domain;
  private TaskList taskList;
  private String identity;
  private String binaryChecksum;
}
