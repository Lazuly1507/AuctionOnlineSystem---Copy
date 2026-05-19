package app.common.dto;

/** AuctionDetailRequest. */
public record AuctionDetailRequest(int auctionId, int knownVersion) implements Request {
  public AuctionDetailRequest(int auctionId) {
    this(auctionId, -1);
  }
}
