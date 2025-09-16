package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CancelTimerFailedEventAttributes {
  private String timerId;
  private String cause;
  private long decisionTaskCompletedEventId;
  private String identity;
}
