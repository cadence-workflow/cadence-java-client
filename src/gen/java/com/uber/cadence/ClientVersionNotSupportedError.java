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
public class ClientVersionNotSupportedError extends BaseError {
  private String featureVersion;
  private String clientImpl;
  private String supportedVersions;

  public ClientVersionNotSupportedError() {
    super();
  }

  public ClientVersionNotSupportedError(String message, Throwable cause) {
    super(message, cause);
  }

  public ClientVersionNotSupportedError(Throwable cause) {
    super(cause);
  }
}
