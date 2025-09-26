package com.uber.cadence;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class TaskListNotOwnedByHostError extends CadenceError {
  private String ownedByIdentity;
  private String myIdentity;
  private String tasklistName;

  public TaskListNotOwnedByHostError() {
    super();
  }

  public TaskListNotOwnedByHostError(String message) {
    super(message);
  }

  public TaskListNotOwnedByHostError(String message, Throwable cause) {
    super(message, cause);
  }

  public TaskListNotOwnedByHostError(Throwable cause) {
    super(cause);
  }
}
