package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApplyParentClosePolicyResult {
  private ApplyParentClosePolicyAttributes child;
  private CrossClusterTaskFailedCause failedCause;
}
