package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RequestCancelActivityTaskFailedEventAttributes {
  private String activityId;
  private String cause;
  private long decisionTaskCompletedEventId;
}
