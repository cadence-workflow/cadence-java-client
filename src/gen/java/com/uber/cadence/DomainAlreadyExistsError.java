package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class DomainAlreadyExistsError extends BaseError {

  public DomainAlreadyExistsError() {
    super();
  }

  public DomainAlreadyExistsError(String message, Throwable cause) {
    super(message, cause);
  }

  public DomainAlreadyExistsError(Throwable cause) {
    super(cause);
  }
}
