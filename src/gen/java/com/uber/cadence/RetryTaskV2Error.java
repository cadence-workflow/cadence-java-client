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
public class RetryTaskV2Error extends BaseError {
  private String domainId;
  private String workflowId;
  private String runId;
  private long startEventId;
  private long startEventVersion;
  private long endEventId;
  private long endEventVersion;

  public RetryTaskV2Error() {
    super();
  }

  public RetryTaskV2Error(String message, Throwable cause) {
    super(message, cause);
  }

  public RetryTaskV2Error(Throwable cause) {
    super(cause);
  }
}
