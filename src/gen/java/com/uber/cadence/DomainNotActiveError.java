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
public class DomainNotActiveError extends BaseError {
  private String domainName;
  private String currentCluster;
  private String activeCluster;

  public DomainNotActiveError() {
    super();
  }

  public DomainNotActiveError(String message, Throwable cause) {
    super(message, cause);
  }

  public DomainNotActiveError(Throwable cause) {
    super(cause);
  }
}
