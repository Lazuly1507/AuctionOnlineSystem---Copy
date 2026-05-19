package app.common.dto;

import java.util.List;

/** AuctionHistoryResponse. */
public record AuctionHistoryResponse(List<AuctionSummary> auctions) implements Response {}
