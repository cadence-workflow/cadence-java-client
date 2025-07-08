package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CancellationAlreadyRequestedError extends BaseError {

  public CancellationAlreadyRequestedError() {
    super();
  }

  public CancellationAlreadyRequestedError(String message, Throwable cause) {
    super(message, cause);
  }

  public CancellationAlreadyRequestedError(Throwable cause) {
    super(cause);
  }
}
