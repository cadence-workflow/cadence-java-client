package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowQueryResult {
  private QueryResultType resultType;
  private byte[] answer;
  private String errorMessage;
}
