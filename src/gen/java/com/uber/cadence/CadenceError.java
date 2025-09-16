package com.uber.cadence;

public class CadenceError extends RuntimeException {
  public CadenceError() {
    super();
  }

  public CadenceError(String message) {
    super(message);
  }

  public CadenceError(String message, Throwable cause) {
    super(message, cause);
  }

  public CadenceError(Throwable cause) {
    super(cause);
  }
}
