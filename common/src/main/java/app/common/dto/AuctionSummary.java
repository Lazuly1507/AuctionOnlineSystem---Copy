package app.common.dto;

import app.common.enums.AuctionStatus;
import java.time.LocalDateTime;

/** AuctionSummary. */
public record AuctionSummary(
    int auctionId,
    String itemName,
    long currentPrice,
    LocalDateTime endTime,
    AuctionStatus status,
    int version) {}
