package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespondActivityTaskCompletedByIDRequest {
  private String domain;
  private String workflowID;
  private String runID;
  private String activityID;
  private byte[] result;
  private String identity;
}
