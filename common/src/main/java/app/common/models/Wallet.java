package app.common.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/** Wallet. */
public class Wallet {
  private static final Gson GSON = new GsonBuilder().create();
  private static final Type FROZEN_TYPE = new TypeToken<Map<String, BigDecimal>>() {}.getType();
  private final ReentrantLock lock = new ReentrantLock();
  private BigDecimal availableBalance;
  private final Map<String, BigDecimal> frozenFunds;

  /** Wallet. */
  public Wallet() {
    this(BigDecimal.ZERO, new HashMap<>());
  }

  /** Wallet. */
  public Wallet(BigDecimal availableBalance) {
    this(availableBalance, new HashMap<>());
  }

  /** Wallet. */
  public Wallet(BigDecimal availableBalance, Map<String, BigDecimal> frozenFunds) {
    this.availableBalance = normalize(availableBalance);
    this.frozenFunds = new HashMap<>();
    if (frozenFunds != null) {
      for (Map.Entry<String, BigDecimal> entry : frozenFunds.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          this.frozenFunds.put(entry.getKey(), normalize(entry.getValue()));
        }
      }
    }
  }

  /** getAvailableBalance. */
  public BigDecimal getAvailableBalance() {
    lock.lock();
    try {
      return availableBalance;
    } finally {
      lock.unlock();
    }
  }

  /** getFrozenFundsSnapshot. */
  public Map<String, BigDecimal> getFrozenFundsSnapshot() {
    lock.lock();
    try {
      return Collections.unmodifiableMap(new HashMap<>(frozenFunds));
    } finally {
      lock.unlock();
    }
  }

  /** getTotalBalance. */
  public BigDecimal getTotalBalance() {
    lock.lock();
    try {
      BigDecimal total = availableBalance;
      for (BigDecimal amount : frozenFunds.values()) {
        total = total.add(amount);
      }
      return total;
    } finally {
      lock.unlock();
    }
  }

  /** getFrozenAmount. */
  public BigDecimal getFrozenAmount(String auctionId) {
    lock.lock();
    try {
      return frozenFunds.getOrDefault(auctionId, BigDecimal.ZERO);
    } finally {
      lock.unlock();
    }
  }

  /** deposit. */
  public void deposit(BigDecimal amount) {
    BigDecimal normalized = normalize(amount);
    if (normalized.signum() <= 0) {
      throw new IllegalArgumentException("So tien gui phai la so duong.");
    }
    lock.lock();
    try {
      availableBalance = availableBalance.add(normalized);
    } finally {
      lock.unlock();
    }
  }

  /** withdraw. */
  public void withdraw(BigDecimal amount) {
    BigDecimal normalized = normalize(amount);
    if (normalized.signum() <= 0) {
      throw new IllegalArgumentException("So tien rut phai la so duong.");
    }
    lock.lock();
    try {
      if (availableBalance.compareTo(normalized) < 0) {
        throw new IllegalArgumentException("So tien rut vuot qua so du.");
      }
      availableBalance = availableBalance.subtract(normalized);
    } finally {
      lock.unlock();
    }
  }

  /** setFrozenAmount. */
  public BigDecimal setFrozenAmount(String auctionId, BigDecimal newAmount) {
    Objects.requireNonNull(auctionId, "auctionId");
    BigDecimal normalized = normalize(newAmount);
    if (normalized.signum() < 0) {
      throw new IllegalArgumentException("So tien dong bang khong hop le.");
    }
    lock.lock();
    try {
      BigDecimal previous = frozenFunds.getOrDefault(auctionId, BigDecimal.ZERO);
      BigDecimal delta = normalized.subtract(previous);
      if (delta.signum() > 0) {
        if (availableBalance.compareTo(delta) < 0) {
          throw new IllegalArgumentException("So du khong du de dat gia.");
        }
        availableBalance = availableBalance.subtract(delta);
      } else if (delta.signum() < 0) {
        availableBalance = availableBalance.add(delta.abs());
      }
      if (normalized.signum() == 0) {
        frozenFunds.remove(auctionId);
      } else {
        frozenFunds.put(auctionId, normalized);
      }
      return previous;
    } finally {
      lock.unlock();
    }
  }

  /** releaseFrozen. */
  public BigDecimal releaseFrozen(String auctionId) {
    Objects.requireNonNull(auctionId, "auctionId");
    lock.lock();
    try {
      BigDecimal amount = frozenFunds.remove(auctionId);
      if (amount != null && amount.signum() > 0) {
        availableBalance = availableBalance.add(amount);
        return amount;
      }
      return BigDecimal.ZERO;
    } finally {
      lock.unlock();
    }
  }

  /** commitFrozen. */
  public BigDecimal commitFrozen(String auctionId) {
    Objects.requireNonNull(auctionId, "auctionId");
    lock.lock();
    try {
      BigDecimal amount = frozenFunds.remove(auctionId);
      return amount == null ? BigDecimal.ZERO : amount;
    } finally {
      lock.unlock();
    }
  }

  /** serializeFrozenFunds. */
  public String serializeFrozenFunds() {
    lock.lock();
    try {
      return GSON.toJson(frozenFunds, FROZEN_TYPE);
    } finally {
      lock.unlock();
    }
  }

  /** parseFrozenFunds. */
  public static Map<String, BigDecimal> parseFrozenFunds(String json) {
    if (json == null || json.isBlank()) {
      return new HashMap<>();
    }
    Map<String, BigDecimal> parsed = GSON.fromJson(json, FROZEN_TYPE);
    return parsed == null ? new HashMap<>() : new HashMap<>(parsed);
  }

  private static BigDecimal normalize(BigDecimal amount) {
    if (amount == null) {
      return BigDecimal.ZERO;
    }
    return amount.stripTrailingZeros();
  }
}
