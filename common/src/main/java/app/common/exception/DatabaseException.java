package app.common.exception;

import java.io.Serial;

/** DatabaseException. */
public class DatabaseException extends AppException {
  /** DatabaseException. */
  @Serial private static final long serialVersionUID = 1L;

  /** DatabaseException. */
  public DatabaseException(String message) {
    super(message);
  }

  /** DatabaseException. */
  public DatabaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
