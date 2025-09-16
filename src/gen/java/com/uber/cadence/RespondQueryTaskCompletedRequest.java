package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RespondQueryTaskCompletedRequest {
  private byte[] taskToken;
  private QueryTaskCompletedType completedType;
  private byte[] queryResult;
  private String errorMessage;
  private WorkerVersionInfo workerVersionInfo;
}
