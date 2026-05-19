package app.server.service;

import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.User;
import app.server.dao.UserDAO;
import app.server.database.TransactionManager;
import app.server.utils.PasswordUtils;
import java.math.BigDecimal;
import java.util.List;

/** UserService. */
public class UserService {
  private final UserDAO userDAO;
  private final TransactionManager transactionManager;

  /** UserService. */
  public UserService(UserDAO userDAO, TransactionManager transactionManager) {
    this.userDAO = userDAO;
    this.transactionManager = transactionManager;
  }

  /** login. */
  public User login(String username, String rawPassword) {
    User user = userDAO.findByUsername(username).orElse(null);
    if (user == null || !PasswordUtils.verify(rawPassword, user.getAccount().getPassword())) {
      throw new ServiceException("Tên đăng nhập hoặc mật khẩu không đúng");
    }
    return user;
  }

  /** register. */
  public User register(User user) {
    validateNotBlank(user.getAccount().getUsername(), "Tên đăng nhập");
    validateNotBlank(user.getAccount().getPassword(), "Mật khẩu");
    validateNotBlank(user.getName(), "Họ tên");
    return transactionManager.runInTransaction(
        conn -> {
          if (userDAO.findByUsername(conn, user.getAccount().getUsername()).isPresent()) {
            throw new ServiceException("User đã tồn tại: " + user.getAccount().getUsername());
          }
          // Hash password in service layer before persisting (single responsibility)
          String hashed = PasswordUtils.hashPassword(user.getAccount().getPassword());
          user.getAccount().setPassword(hashed);
          return userDAO.save(conn, user);
        });
  }

  /** updateProfile. */
  public void updateProfile(User user) {
    validateNotBlank(user.getName(), "Họ tên");
    transactionManager.runWithoutResult(
        conn -> {
          User stored =
              userDAO
                  .findById(conn, user.getId())
                  .orElseThrow(
                      () -> new ServiceException("Không tìm thấy user với id: " + user.getId()));
          stored.setName(user.getName());
          userDAO.update(conn, stored);
        });
  }

  /** changePassword. */
  public void changePassword(String username, String oldPassword, String newPassword) {
    validateNotBlank(newPassword, "Mật khẩu mới");
    User user = login(username, oldPassword);
    String hashed = PasswordUtils.hashPassword(newPassword);
    transactionManager.runWithoutResult(
        conn -> {
          user.getAccount().setPassword(hashed);
          userDAO.update(conn, user);
        });
  }

  /** getById. */
  public User getById(int userId) {
    return userDAO
        .findById(userId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
  }

  /** getAllUsers. */
  public List<User> getAllUsers(int requesterId) {
    User requester = getById(requesterId);
    if (requester.getRole() != UserRole.ADMIN) {
      throw new ServiceException("Chỉ Admin được xem danh sách người dùng.");
    }
    return userDAO.findAll();
  }

  /** deposit. */
  public User deposit(int userId, BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) {
      throw new ServiceException("Số tiền nạp phải > 0");
    }
    return transactionManager.runInTransaction(
        conn -> {
          userDAO.lockRow(conn, userId);
          User user =
              userDAO
                  .findById(conn, userId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
          try {
            user.getWallet().deposit(amount);
          } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
          }
          userDAO.update(conn, user);
          return user;
        });
  }

  /** withdraw. */
  public void withdraw(String username, String password, BigDecimal amount) {
    User user = login(username, password);
    if (amount == null || amount.signum() <= 0) {
      throw new ServiceException("Số tiền rút phải > 0");
    }
    transactionManager.runWithoutResult(
        conn -> {
          userDAO.lockRow(conn, user.getId());
          User stored =
              userDAO
                  .findById(conn, user.getId())
                  .orElseThrow(
                      () -> new ServiceException("Không tìm thấy user với id: " + user.getId()));
          try {
            stored.getWallet().withdraw(amount);
          } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
          }
          userDAO.update(conn, stored);
        });
  }

  /** reserveBidAmount. */
  public BigDecimal reserveBidAmount(int userId, int auctionId, BigDecimal bidAmount) {
    if (bidAmount == null || bidAmount.signum() <= 0) {
      throw new ServiceException("Giá đặt không hợp lệ.");
    }
    return transactionManager.runInTransaction(
        conn -> {
          userDAO.lockRow(conn, userId);
          User user =
              userDAO
                  .findById(conn, userId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
          try {
            BigDecimal previous =
                user.getWallet().setFrozenAmount(String.valueOf(auctionId), bidAmount);
            userDAO.update(conn, user);
            return previous;
          } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
          }
        });
  }

  /** restoreFrozenAmount. */
  public User restoreFrozenAmount(int userId, int auctionId, BigDecimal previousAmount) {
    return transactionManager.runInTransaction(
        conn -> {
          userDAO.lockRow(conn, userId);
          User user =
              userDAO
                  .findById(conn, userId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
          try {
            user.getWallet().setFrozenAmount(String.valueOf(auctionId), previousAmount);
          } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
          }
          userDAO.update(conn, user);
          return user;
        });
  }

  /** settleFrozenAmount. */
  public User settleFrozenAmount(int userId, int auctionId, boolean winner) {
    return transactionManager.runInTransaction(
        conn -> {
          userDAO.lockRow(conn, userId);
          User user =
              userDAO
                  .findById(conn, userId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
          if (winner) {
            user.getWallet().commitFrozen(String.valueOf(auctionId));
          } else {
            user.getWallet().releaseFrozen(String.valueOf(auctionId));
          }
          userDAO.update(conn, user);
          return user;
        });
  }

  private void validateNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new ServiceException(fieldName + " không được để trống.");
    }
  }
}
