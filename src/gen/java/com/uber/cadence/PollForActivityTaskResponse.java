package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PollForActivityTaskResponse {
  private byte[] taskToken;
  private WorkflowExecution workflowExecution;
  private String activityId;
  private ActivityType activityType;
  private byte[] input;
  private long scheduledTimestamp;
  private int scheduleToCloseTimeoutSeconds;
  private long startedTimestamp;
  private int startToCloseTimeoutSeconds;
  private int heartbeatTimeoutSeconds;
  private int attempt;
  private long scheduledTimestampOfThisAttempt;
  private byte[] heartbeatDetails;
  private WorkflowType workflowType;
  private String workflowDomain;
  private Header header;
}
