package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespondDecisionTaskFailedRequest {
  private byte[] taskToken;
  private DecisionTaskFailedCause cause;
  private byte[] details;
  private String identity;
  private String binaryChecksum;
}
