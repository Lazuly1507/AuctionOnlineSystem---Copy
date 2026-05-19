package app.common.dto;

/** AuctionResultResponse. */
public record AuctionResultResponse(long auctionId, ProfileData winner, long finalPrice)
    implements Response {}
