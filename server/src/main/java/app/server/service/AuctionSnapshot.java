package app.server.service;

import app.common.models.Auction;
import app.common.models.Item;

/** Server-side auction projection used before mapping to transport DTOs. */
public record AuctionSnapshot(Auction auction, Item item) {
  public int auctionId() {
    return auction.getId();
  }

  public int version() {
    return auction.getVersion();
  }
}
