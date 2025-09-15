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
public class EntityNotExistsError extends BaseError {
  private String currentCluster;
  private String activeCluster;

  public EntityNotExistsError() {
    super();
  }

  public EntityNotExistsError(String message) {
    super(message);
  }

  public EntityNotExistsError(String message, Throwable cause) {
    super(message, cause);
  }

  public EntityNotExistsError(Throwable cause) {
    super(cause);
  }
}
