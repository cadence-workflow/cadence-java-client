package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class InternalDataInconsistencyError extends CadenceError {

  public InternalDataInconsistencyError() {
    super();
  }

  public InternalDataInconsistencyError(String message) {
    super(message);
  }

  public InternalDataInconsistencyError(String message, Throwable cause) {
    super(message, cause);
  }

  public InternalDataInconsistencyError(Throwable cause) {
    super(cause);
  }
}
