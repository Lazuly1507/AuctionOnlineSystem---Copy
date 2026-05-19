package app.common.models;

import java.time.LocalDateTime;

/** Bid. */
public class Bid {
  private int id;
  private int auctionId;
  private int bidderId;
  private String bidderName;
  private long amount;
  private LocalDateTime createAt;
  private boolean isAutoBid;

  /** Bid. */
  public Bid(
      int id,
      int auctionId,
      int bidderId,
      String bidderName,
      long amount,
      LocalDateTime createAt,
      boolean isAutoBid) {
    this.id = id;
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.bidderName = bidderName;
    this.amount = amount;
    this.createAt = createAt;
    this.isAutoBid = isAutoBid;
  }

  public int getId() {
    return id;
  }

  public String getBidderName() {
    return bidderName;
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

  public int getBidderId() {
    return bidderId;
  }

  public void setBidderId(int bidderId) {
    this.bidderId = bidderId;
  }

  public long getAmount() {
    return amount;
  }

  public void setAmount(long amount) {
    this.amount = amount;
  }

  public LocalDateTime getCreateAt() {
    return createAt;
  }

  @Override
  public String toString() {
    return bidderId + " đã trả $" + amount + " vào lúc " + createAt.withNano(0);
  }

  public boolean isAutoBid() {
    return isAutoBid;
  }

  public void setAutoBid(boolean autoBid) {
    isAutoBid = autoBid;
  }
}
