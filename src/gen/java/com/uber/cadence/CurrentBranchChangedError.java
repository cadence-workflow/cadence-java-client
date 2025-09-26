package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CurrentBranchChangedError extends CadenceError {
  private byte[] currentBranchToken;

  public CurrentBranchChangedError() {
    super();
  }

  public CurrentBranchChangedError(String message) {
    super(message);
  }

  public CurrentBranchChangedError(String message, Throwable cause) {
    super(message, cause);
  }

  public CurrentBranchChangedError(Throwable cause) {
    super(cause);
  }
}
