package app;

import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.models.Account;
import app.common.models.Auction;
import app.common.models.Item;
import app.common.models.ItemFactory;
import app.common.models.User;
import app.common.models.UserFactory;
import app.common.models.Wallet;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestFixtures {
  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  private TestFixtures() {}

  public static String unique(String prefix) {
    return prefix + "_" + SEQUENCE.incrementAndGet();
  }

  public static User user(String username, UserRole role) {
    return user(username, role, new BigDecimal("1000000"));
  }

  public static User user(String username, UserRole role, BigDecimal balance) {
    return UserFactory.createUser(
        "Test " + username, new Account(username, "password"), new Wallet(balance), role);
  }

  public static Item item(int sellerId, String name, ItemType type) {
    return ItemFactory.createItem(name, sellerId, "Test item", 1000L, 100L, type);
  }

  public static Auction auction(int itemId, int sellerId, LocalDateTime endTime, long price) {
    return new Auction(itemId, sellerId, endTime, price);
  }
}
