package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BadBinaryInfo {
  private String reason;
  private String operator;
  private long createdTimeNano;
}
