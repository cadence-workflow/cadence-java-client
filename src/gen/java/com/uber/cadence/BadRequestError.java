package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class BadRequestError extends CadenceError {

  public BadRequestError() {
    super();
  }

  public BadRequestError(String message) {
    super(message);
  }

  public BadRequestError(String message, Throwable cause) {
    super(message, cause);
  }

  public BadRequestError(Throwable cause) {
    super(cause);
  }
}
