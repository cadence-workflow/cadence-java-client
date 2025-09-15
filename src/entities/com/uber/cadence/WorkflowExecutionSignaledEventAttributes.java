package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowExecutionSignaledEventAttributes {
  private String signalName;
  private byte[] input;
  private String identity;
  private String requestId;
}
