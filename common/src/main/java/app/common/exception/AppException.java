package app.common.exception;

import java.io.Serial;

/** AppException. */
public class AppException extends RuntimeException {
  /** AppException. */
  @Serial private static final long serialVersionUID = 1L;

  /** AppException. */
  public AppException(String message) {
    super(message);
  }

  /** AppException. */
  public AppException(String message, Throwable cause) {
    super(message, cause);
  }
}
