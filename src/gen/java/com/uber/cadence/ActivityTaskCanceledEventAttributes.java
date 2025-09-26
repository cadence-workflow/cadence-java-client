package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ActivityTaskCanceledEventAttributes {
  private byte[] details;
  private long latestCancelRequestedEventId;
  private long scheduledEventId;
  private long startedEventId;
  private String identity;
}
