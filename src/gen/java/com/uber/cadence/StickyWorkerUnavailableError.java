package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class StickyWorkerUnavailableError extends CadenceError {

  public StickyWorkerUnavailableError() {
    super();
  }

  public StickyWorkerUnavailableError(String message) {
    super(message);
  }

  public StickyWorkerUnavailableError(String message, Throwable cause) {
    super(message, cause);
  }

  public StickyWorkerUnavailableError(Throwable cause) {
    super(cause);
  }
}
