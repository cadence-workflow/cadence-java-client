package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class QueueState {
  private Map<Long, VirtualQueueState> virtualQueueStates = new HashMap<>();;
  private TaskKey exclusiveMaxReadLevel;
}
