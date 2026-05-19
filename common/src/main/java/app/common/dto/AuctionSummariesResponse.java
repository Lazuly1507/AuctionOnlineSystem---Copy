package app.common.dto;

import java.util.List;

/** AuctionSummariesResponse. */
public record AuctionSummariesResponse(List<AuctionSummary> auctions) implements Response {}
