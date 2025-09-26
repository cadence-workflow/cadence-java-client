package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespondActivityTaskCompletedRequest {
  private byte[] taskToken;
  private byte[] result;
  private String identity;
}
