package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DecisionTaskStartedEventAttributes {
  private long scheduledEventId;
  private String identity;
  private String requestId;
}
