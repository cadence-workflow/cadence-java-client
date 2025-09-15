package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class RemoteSyncMatchedError extends BaseError {

  public RemoteSyncMatchedError() {
    super();
  }

  public RemoteSyncMatchedError(String message, Throwable cause) {
    super(message, cause);
  }

  public RemoteSyncMatchedError(Throwable cause) {
    super(cause);
  }
}
