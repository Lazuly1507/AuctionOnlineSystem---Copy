package app.common.models;

import java.time.LocalDateTime;

/** AutoBid. */
public class AutoBid {
  private int id;
  private int auctionId;
  private int userId;
  private long maxAmount;
  private long incrementAmount;
  private boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** AutoBid. */
  public AutoBid() {}

  /** AutoBid. */
  public AutoBid(
      int id,
      int auctionId,
      int userId,
      long maxAmount,
      long incrementAmount,
      boolean enabled,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.auctionId = auctionId;
    this.userId = userId;
    this.maxAmount = maxAmount;
    this.incrementAmount = incrementAmount;
    this.enabled = enabled;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public long getMaxAmount() {
    return maxAmount;
  }

  public void setMaxAmount(long maxAmount) {
    this.maxAmount = maxAmount;
  }

  public long getIncrementAmount() {
    return incrementAmount;
  }

  public void setIncrementAmount(long incrementAmount) {
    this.incrementAmount = incrementAmount;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
