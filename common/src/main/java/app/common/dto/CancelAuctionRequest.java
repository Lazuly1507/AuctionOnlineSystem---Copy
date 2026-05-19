package app.common.dto;

/** CancelAuctionRequest. */
public record CancelAuctionRequest(int auctionId, int expectedVersion) implements Request {}
