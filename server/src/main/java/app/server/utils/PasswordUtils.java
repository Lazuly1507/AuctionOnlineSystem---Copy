package app.server.utils;

import app.common.exception.ServiceException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/** PasswordUtils. */
public class PasswordUtils {
  /** hashPassword. */
  public static String hashPassword(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedHash = digest.digest(password.getBytes());
      return Base64.getEncoder().encodeToString(encodedHash);
    } catch (NoSuchAlgorithmException e) {
      throw new ServiceException("Lỗi thuật toán mã hóa", e);
    }
  }

  /** verify. */
  public static boolean verify(String plainTextPassword, String hashedPasswordInDb) {
    String hashedInput = hashPassword(plainTextPassword);
    return hashedInput.equals(hashedPasswordInDb);
  }
}
