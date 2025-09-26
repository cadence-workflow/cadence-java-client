package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespondActivityTaskCanceledRequest {
  private byte[] taskToken;
  private byte[] details;
  private String identity;
}
