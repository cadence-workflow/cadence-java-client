package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class WorkflowExecutionAlreadyCompletedError extends BaseError {

  public WorkflowExecutionAlreadyCompletedError() {
    super();
  }

  public WorkflowExecutionAlreadyCompletedError(String message) {
    super(message);
  }

  public WorkflowExecutionAlreadyCompletedError(String message, Throwable cause) {
    super(message, cause);
  }

  public WorkflowExecutionAlreadyCompletedError(Throwable cause) {
    super(cause);
  }
}
