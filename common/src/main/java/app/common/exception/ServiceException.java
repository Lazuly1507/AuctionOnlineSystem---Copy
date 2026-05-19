package app.common.exception;

import java.io.Serial;

/** ServiceException. */
public class ServiceException extends AppException {
  /** ServiceException. */
  @Serial private static final long serialVersionUID = 1L;

  /** ServiceException. */
  public ServiceException(String message) {
    super(message);
  }

  /** ServiceException. */
  public ServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
