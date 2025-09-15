package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TimerCanceledEventAttributes {
  private String timerId;
  private long startedEventId;
  private long decisionTaskCompletedEventId;
  private String identity;
}
