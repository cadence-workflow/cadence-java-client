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
public class CurrentBranchChangedError extends BaseError {
  private byte[] currentBranchToken;

  public CurrentBranchChangedError() {
    super();
  }

  public CurrentBranchChangedError(String message, Throwable cause) {
    super(message, cause);
  }

  public CurrentBranchChangedError(Throwable cause) {
    super(cause);
  }
}
