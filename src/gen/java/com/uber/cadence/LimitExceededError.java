package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class LimitExceededError extends CadenceError {

  public LimitExceededError() {
    super();
  }

  public LimitExceededError(String message) {
    super(message);
  }

  public LimitExceededError(String message, Throwable cause) {
    super(message, cause);
  }

  public LimitExceededError(Throwable cause) {
    super(cause);
  }
}
