package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class ServiceBusyError extends CadenceError {
  private String reason;

  public ServiceBusyError() {
    super();
  }

  public ServiceBusyError(String message) {
    super(message);
  }

  public ServiceBusyError(String message, Throwable cause) {
    super(message, cause);
  }

  public ServiceBusyError(Throwable cause) {
    super(cause);
  }
}
