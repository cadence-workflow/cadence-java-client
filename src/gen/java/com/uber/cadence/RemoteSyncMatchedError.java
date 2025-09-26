package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class RemoteSyncMatchedError extends CadenceError {

  public RemoteSyncMatchedError() {
    super();
  }

  public RemoteSyncMatchedError(String message) {
    super(message);
  }

  public RemoteSyncMatchedError(String message, Throwable cause) {
    super(message, cause);
  }

  public RemoteSyncMatchedError(Throwable cause) {
    super(cause);
  }
}
