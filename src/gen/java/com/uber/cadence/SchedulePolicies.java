package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SchedulePolicies {
  private ScheduleOverlapPolicy overlapPolicy;
  private ScheduleCatchUpPolicy catchUpPolicy;
  private int catchUpWindowInSeconds;
  private boolean pauseOnFailure;
  private int bufferLimit;
  private int concurrencyLimit;
}
