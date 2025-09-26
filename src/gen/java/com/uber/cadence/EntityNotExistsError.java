package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class EntityNotExistsError extends CadenceError {
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
