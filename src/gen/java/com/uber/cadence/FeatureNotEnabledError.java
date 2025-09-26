package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class FeatureNotEnabledError extends CadenceError {
  private String featureFlag;

  public FeatureNotEnabledError() {
    super();
  }

  public FeatureNotEnabledError(String message) {
    super(message);
  }

  public FeatureNotEnabledError(String message, Throwable cause) {
    super(message, cause);
  }

  public FeatureNotEnabledError(Throwable cause) {
    super(cause);
  }
}
