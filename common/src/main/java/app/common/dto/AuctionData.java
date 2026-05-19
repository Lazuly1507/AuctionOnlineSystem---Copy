package app.common.dto;

import app.common.enums.AuctionStatus;
import java.time.LocalDateTime;

/** AuctionData. */
public record AuctionData(
    int id,
    int itemId,
    int sellerId,
    Integer winnerId,
    AuctionStatus status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    long highestBid,
    int extendedCount,
    int version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
