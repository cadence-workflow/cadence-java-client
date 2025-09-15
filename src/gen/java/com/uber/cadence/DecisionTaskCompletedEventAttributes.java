package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DecisionTaskCompletedEventAttributes {
  private byte[] executionContext;
  private long scheduledEventId;
  private long startedEventId;
  private String identity;
  private String binaryChecksum;
}
