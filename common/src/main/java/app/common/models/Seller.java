package app.common.models;

import app.common.enums.UserRole;

/** Seller. */
public class Seller extends User {
  /** Seller. */
  public Seller(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
  }

  /** Seller. */
  public Seller(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
  }

  @Override
  public UserRole getRole() {
    return UserRole.SELLER;
  }
}
