package com.uber.cadence;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class ServiceBusyError extends BaseError {
  private String reason;

  public ServiceBusyError() {
    super();
  }

  public ServiceBusyError(String message, Throwable cause) {
    super(message, cause);
  }

  public ServiceBusyError(Throwable cause) {
    super(cause);
  }
}
