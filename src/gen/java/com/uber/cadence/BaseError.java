package com.uber.cadence;

public class BaseError extends RuntimeException {
  public BaseError() {
    super();
  }

  public BaseError(String message) {
    super(message);
  }

  public BaseError(String message, Throwable cause) {
    super(message, cause);
  }

  public BaseError(Throwable cause) {
    super(cause);
  }
}
