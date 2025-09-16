package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RecordActivityTaskHeartbeatByIDRequest {
  private String domain;
  private String workflowID;
  private String runID;
  private String activityID;
  private byte[] details;
  private String identity;
}
