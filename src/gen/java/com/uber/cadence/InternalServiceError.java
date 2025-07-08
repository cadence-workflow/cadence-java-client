package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class InternalServiceError extends BaseError {

  public InternalServiceError() {
    super();
  }

  public InternalServiceError(String message, Throwable cause) {
    super(message, cause);
  }

  public InternalServiceError(Throwable cause) {
    super(cause);
  }
}
