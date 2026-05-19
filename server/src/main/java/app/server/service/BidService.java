package app.server.service;

import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.User;
import app.server.dao.AuctionDAO;
import app.server.dao.BidDAO;
import app.server.dao.UserDAO;
import app.server.database.TransactionManager;
import java.math.BigDecimal;

/** BidService. */
public class BidService {
  private final BidDAO bidDAO;
  private final AuctionDAO auctionDAO;
  private final UserDAO userDAO;
  private final TransactionManager transactionManager;
  private final BidValidator bidValidator;
  private final AntiSnipeService antiSnipeService;

  /** BidService. */
  public BidService(
      BidDAO bidDAO,
      AuctionDAO auctionDAO,
      UserDAO userDAO,
      TransactionManager transactionManager,
      BidValidator bidValidator,
      AntiSnipeService antiSnipeService) {
    this.bidDAO = bidDAO;
    this.auctionDAO = auctionDAO;
    this.userDAO = userDAO;
    this.transactionManager = transactionManager;
    this.bidValidator = bidValidator;
    this.antiSnipeService = antiSnipeService;
  }

  /** placeBid. */
  public Auction placeBid(int auctionId, int userId, long bidAmount) {
    return transactionManager.runInTransaction(
        conn -> {
          auctionDAO.lockRow(conn, auctionId);
          Auction auction =
              auctionDAO
                  .findById(conn, auctionId)
                  .orElseThrow(() -> new ServiceException("Phiên đấu giá không tồn tại."));
          if (auction.getSellerId() == userId) {
            throw new ServiceException("Người bán không được tự đặt giá sản phẩm của mình.");
          }
          bidValidator.validateAuctionState(auction);
          bidValidator.validateBidAmount(bidAmount, auction.getHighestBid());
          userDAO.lockRow(conn, userId);
          User bidder =
              userDAO
                  .findById(conn, userId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
          try {
            bidder
                .getWallet()
                .setFrozenAmount(String.valueOf(auctionId), BigDecimal.valueOf(bidAmount));
          } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
          }
          auction.updateHighestBid(bidAmount, userId);
          antiSnipeService.apply(auction);
          bidDAO.insertBid(conn, auctionId, userId, bidAmount, false);
          userDAO.update(conn, bidder);
          auctionDAO.update(conn, auction);
          return auction;
        });
  }
}
