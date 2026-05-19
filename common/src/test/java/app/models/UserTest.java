package app.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.common.enums.UserRole;
import app.common.models.Account;
import app.common.models.User;
import app.common.models.UserFactory;
import app.common.models.Wallet;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserTest {
  private static final Logger logger = LoggerFactory.getLogger(UserTest.class);
  private User user;

  @BeforeEach
  public void setUser() {
    logger.info("Setting up a new User for testing...");
    try {
      user =
          UserFactory.createUser(
              "Tester",
              new Account("test_account", "test_password"),
              new Wallet(),
              UserRole.BIDDER);
      logger.info("Test User created successfully.");
    } catch (Exception e) {
      logger.error("Failed to create Test User", e);
      throw e;
    }
  }

  @Test
  public void Deposit_Positive() {
    logger.info("Running test: Deposit_Positive");
    user.getWallet().deposit(new BigDecimal("100000"));
    assertEquals(0, user.getWallet().getAvailableBalance().compareTo(new BigDecimal("100000")));
    logger.info("Test passed: Positive deposit verified");
  }

  @Test
  public void Deposit_Negative() {
    logger.warn("Running test: Deposit_Negative - expecting exception for negative value");
    assertThrows(
        IllegalArgumentException.class, () -> user.getWallet().deposit(new BigDecimal("-100000")));
    logger.info("Test passed: Exception thrown for negative deposit");
  }

  @Test
  public void Withdraw_Success() {
    logger.info("Running test: Withdraw_Success");
    user.getWallet().deposit(new BigDecimal("100000"));
    user.getWallet().withdraw(new BigDecimal("50000"));
    assertEquals(0, user.getWallet().getAvailableBalance().compareTo(new BigDecimal("50000")));
    logger.info("Test passed: Withdrawal verified");
  }

  @Test
  public void Withdraw_Fail() {
    logger.warn("Running test: Withdraw_Fail - expecting exception for insufficient funds");
    assertThrows(
        IllegalArgumentException.class, () -> user.getWallet().withdraw(new BigDecimal("50000")));
    logger.info("Test passed: Exception thrown for insufficient funds");
  }

  @Test
  public void Withdraw_Negative() {
    logger.warn("Running test: Withdraw_Negative - expecting exception for negative value");
    assertThrows(
        IllegalArgumentException.class, () -> user.getWallet().withdraw(new BigDecimal("-50000")));
    logger.info("Test passed: Exception thrown for negative withdrawal");
  }
}
