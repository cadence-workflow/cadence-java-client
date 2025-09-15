package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class QueryFailedError extends CadenceError {

  public QueryFailedError() {
    super();
  }

  public QueryFailedError(String message) {
    super(message);
  }

  public QueryFailedError(String message, Throwable cause) {
    super(message, cause);
  }

  public QueryFailedError(Throwable cause) {
    super(cause);
  }
}
