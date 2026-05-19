package app.common.models;

import app.common.enums.UserRole;

/** Admin. */
public class Admin extends User {
  /** Admin. */
  public Admin(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
  }

  /** Admin. */
  public Admin(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
  }

  @Override
  public UserRole getRole() {
    return UserRole.ADMIN;
  }
}
