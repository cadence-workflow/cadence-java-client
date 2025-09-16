package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DecisionTaskScheduledEventAttributes {
  private TaskList taskList;
  private int startToCloseTimeoutSeconds;
  private long attempt;
}
