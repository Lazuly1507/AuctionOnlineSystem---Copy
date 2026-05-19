package app.common.dto;

/** PlaceBidResponse. */
public record PlaceBidResponse(long auctionId, long highestBidAmount, long bidderId)
    implements Response {}
