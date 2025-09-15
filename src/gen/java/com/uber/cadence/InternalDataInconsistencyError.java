package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class InternalDataInconsistencyError extends BaseError {

  public InternalDataInconsistencyError() {
    super();
  }

  public InternalDataInconsistencyError(String message, Throwable cause) {
    super(message, cause);
  }

  public InternalDataInconsistencyError(Throwable cause) {
    super(cause);
  }
}
