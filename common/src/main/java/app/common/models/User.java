package app.common.models;

import app.common.enums.UserRole;
import java.util.Objects;

/** User. */
public abstract class User extends Entity {
  protected String name;
  protected final Account account;
  protected final Wallet wallet;

  /** User. */
  public User(int id, String name, Account account, Wallet wallet) {
    super(id);
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank.");
    }
    this.name = name;
    this.account = Objects.requireNonNull(account, "account");
    this.wallet = Objects.requireNonNull(wallet, "wallet");
  }

  /** User. */
  public User(String name, Account account, Wallet wallet) {
    super();
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank.");
    }
    this.name = name;
    this.account = Objects.requireNonNull(account, "account");
    this.wallet = Objects.requireNonNull(wallet, "wallet");
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Account getAccount() {
    return account;
  }

  public Wallet getWallet() {
    return wallet;
  }

  /** getRole. */
  public abstract UserRole getRole();
}
