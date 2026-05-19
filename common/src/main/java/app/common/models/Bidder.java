package app.common.models;

import app.common.enums.UserRole;

/** Bidder. */
public class Bidder extends User {
  /** Bidder. */
  public Bidder(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
  }

  /** Bidder. */
  public Bidder(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
  }

  @Override
  public UserRole getRole() {
    return UserRole.BIDDER;
  }
}
