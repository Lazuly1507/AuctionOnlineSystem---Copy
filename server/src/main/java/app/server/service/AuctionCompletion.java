package app.server.service;

import app.common.models.Bid;
import java.util.Optional;
import java.util.Set;

/** Auction completion result. */
public record AuctionCompletion(
    int auctionId, boolean completed, Optional<Bid> highestBid, Set<Integer> settledUserIds) {
  /** AuctionCompletion. */
  public AuctionCompletion {
    highestBid = highestBid == null ? Optional.empty() : highestBid;
    settledUserIds = settledUserIds == null ? Set.of() : Set.copyOf(settledUserIds);
  }
}
