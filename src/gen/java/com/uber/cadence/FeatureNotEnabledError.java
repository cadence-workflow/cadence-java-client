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
public class FeatureNotEnabledError extends BaseError {
  private String featureFlag;

  public FeatureNotEnabledError() {
    super();
  }

  public FeatureNotEnabledError(String message, Throwable cause) {
    super(message, cause);
  }

  public FeatureNotEnabledError(Throwable cause) {
    super(cause);
  }
}
