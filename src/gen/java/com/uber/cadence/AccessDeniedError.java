package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class AccessDeniedError extends BaseError {

  public AccessDeniedError() {
    super();
  }

  public AccessDeniedError(String message, Throwable cause) {
    super(message, cause);
  }

  public AccessDeniedError(Throwable cause) {
    super(cause);
  }
}
