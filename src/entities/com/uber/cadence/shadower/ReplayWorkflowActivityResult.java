package com.uber.cadence.shadower;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ReplayWorkflowActivityResult {
  private int succeeded;
  private int skipped;
  private int failed;
}
