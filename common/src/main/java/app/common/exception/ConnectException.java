package app.common.exception;

import java.io.Serial;

/** ConnectException. */
public class ConnectException extends RuntimeException {
  /** ConnectException. */
  @Serial private static final long serialVersionUID = 1L;

  /** ConnectException. */
  public ConnectException(String message) {
    super(message);
  }

  /** ConnectException. */
  public ConnectException(String message, Throwable cause) {
    super(message, cause);
  }
}
