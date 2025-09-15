package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespondActivityTaskFailedRequest {
  private byte[] taskToken;
  private String reason;
  private byte[] details;
  private String identity;
}
