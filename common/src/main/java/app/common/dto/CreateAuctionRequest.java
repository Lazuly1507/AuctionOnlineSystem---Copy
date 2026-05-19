package app.common.dto;

import app.common.enums.ItemType;

/** CreateAuctionRequest. */
public record CreateAuctionRequest(
    String name,
    String description,
    long startingPrice,
    long stepPrice,
    ItemType type,
    int durationMinutes)
    implements Request {}
