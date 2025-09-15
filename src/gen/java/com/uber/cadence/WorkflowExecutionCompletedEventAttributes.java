package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowExecutionCompletedEventAttributes {
  private byte[] result;
  private long decisionTaskCompletedEventId;
}
