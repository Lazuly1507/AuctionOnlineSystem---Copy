package app.common.dto;

/** AuctionDetailResponse. */
public record AuctionDetailResponse(
    AuctionDetail detail, int auctionId, int version, boolean notModified) implements Response {
  public AuctionDetailResponse(AuctionDetail detail) {
    this(
        detail,
        detail == null ? 0 : detail.auctionId(),
        detail == null ? -1 : detail.version(),
        false);
  }

  public static AuctionDetailResponse notModified(int auctionId, int version) {
    return new AuctionDetailResponse(null, auctionId, version, true);
  }
}
