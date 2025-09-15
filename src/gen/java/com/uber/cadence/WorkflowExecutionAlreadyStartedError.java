package com.uber.cadence;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class WorkflowExecutionAlreadyStartedError extends BaseError {
  private String startRequestId;
  private String runId;

  public WorkflowExecutionAlreadyStartedError() {
    super();
  }

  public WorkflowExecutionAlreadyStartedError(String message) {
    super(message);
  }

  public WorkflowExecutionAlreadyStartedError(String message, Throwable cause) {
    super(message, cause);
  }

  public WorkflowExecutionAlreadyStartedError(Throwable cause) {
    super(cause);
  }
}
