package app.common.models;

import app.common.enums.UserRole;

/** UserFactory. */
public class UserFactory {
  /** createUser. */
  public static User createUser(String name, Account account, Wallet wallet, UserRole role) {
    return switch (role) {
      case ADMIN -> new Admin(name, account, wallet);
      case SELLER -> new Seller(name, account, wallet);
      case BIDDER -> new Bidder(name, account, wallet);
    };
  }

  /** createUser. */
  public static User createUser(
      int id, String name, Account account, Wallet wallet, UserRole role) {
    return switch (role) {
      case ADMIN -> new Admin(id, name, account, wallet);
      case SELLER -> new Seller(id, name, account, wallet);
      case BIDDER -> new Bidder(id, name, account, wallet);
    };
  }
}
