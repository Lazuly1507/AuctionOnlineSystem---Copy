package app.common.dto;

import java.time.LocalDateTime;

/** AuctionDetail. */
public record AuctionDetail(AuctionData auction, ItemData item) {
  public int auctionId() {
    return auction.id();
  }

  public String itemName() {
    return item.name();
  }

  public String description() {
    return item.description();
  }

  public long startingPrice() {
    return item.startingPrice();
  }

  public long stepPrice() {
    return item.stepPrice();
  }

  public LocalDateTime endTime() {
    return auction.endTime();
  }

  public int version() {
    return auction.version();
  }
}
