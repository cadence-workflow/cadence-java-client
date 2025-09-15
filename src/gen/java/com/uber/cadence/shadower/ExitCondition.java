package com.uber.cadence.shadower;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ExitCondition {
  private int expirationIntervalInSeconds;
  private int shadowCount;
}
