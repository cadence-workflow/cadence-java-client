package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ActivityTaskScheduledEventAttributes {
  private String activityId;
  private ActivityType activityType;
  private String domain;
  private TaskList taskList;
  private byte[] input;
  private int scheduleToCloseTimeoutSeconds;
  private int scheduleToStartTimeoutSeconds;
  private int startToCloseTimeoutSeconds;
  private int heartbeatTimeoutSeconds;
  private long decisionTaskCompletedEventId;
  private RetryPolicy retryPolicy;
  private Header header;
}
