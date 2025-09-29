package com.uber.cadence;

import lombok.experimental.Accessors;

@Accessors(chain = true)
public class TimeoutError extends CadenceError {

  public TimeoutError() {
    super();
  }

  public TimeoutError(String message) {
    super(message);
  }

  public TimeoutError(String message, Throwable cause) {
    super(message, cause);
  }

  public TimeoutError(Throwable cause) {
    super(cause);
  }
}
