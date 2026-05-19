package app.server.service;

import app.common.enums.AuctionStatus;
import app.common.exception.ServiceException;
import app.common.models.Auction;

/** BidValidator. */
public class BidValidator {
  private static final long MIN_INCREMENT = 1L;

  /** validateAuctionState. */
  public void validateAuctionState(Auction auction) {
    if (auction.getStatus() != AuctionStatus.RUNNING) {
      throw new ServiceException(
          "Phiên đấu giá đã " + auction.getStatus().name().toLowerCase() + ".");
    }
  }

  /** validateBidAmount. */
  public void validateBidAmount(long bidAmount, long currentPrice) {
    long minimumRequired = currentPrice + MIN_INCREMENT;
    if (bidAmount < minimumRequired) {
      throw new ServiceException("Giá đặt phải tối thiểu " + minimumRequired + " VNĐ.");
    }
  }
}
