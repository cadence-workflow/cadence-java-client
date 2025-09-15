package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RetryPolicy {
  private int initialIntervalInSeconds;
  private double backoffCoefficient;
  private int maximumIntervalInSeconds;
  private int maximumAttempts;
  private List<String> nonRetriableErrorReasons;
  private int expirationIntervalInSeconds;
}
