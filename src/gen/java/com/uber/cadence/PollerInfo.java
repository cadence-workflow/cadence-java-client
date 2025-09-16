package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PollerInfo {
  private long lastAccessTime;
  private String identity;
  private double ratePerSecond;
}
