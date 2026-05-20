package app.server.service;

import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.Bid;
import app.common.models.User;
import app.server.dao.BidDAO;
import app.server.dao.UserDAO;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Handles wallet settlement/release for auction participants. */
public class AuctionSettlementService {
  private final BidDAO bidDAO;
  private final UserDAO userDAO;

  public AuctionSettlementService(BidDAO bidDAO, UserDAO userDAO) {
    this.bidDAO = bidDAO;
    this.userDAO = userDAO;
  }

  public Set<Integer> settleWallets(java.sql.Connection conn, Auction auction) {
    Set<Integer> bidderIds = bidderIds(conn, auction.getId());
    for (Integer bidderId : bidderIds) {
      userDAO.lockRow(conn, bidderId);
      User user =
          userDAO
              .findById(conn, bidderId)
              .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + bidderId));
      if (auction.getWinnerId() != null && auction.getWinnerId() == bidderId) {
        user.getWallet().commitFrozen(String.valueOf(auction.getId()));
      } else {
        user.getWallet().releaseFrozen(String.valueOf(auction.getId()));
      }
      userDAO.update(conn, user);
    }
    return bidderIds;
  }

  public void releaseWallets(java.sql.Connection conn, Auction auction) {
    Set<Integer> bidderIds = bidderIds(conn, auction.getId());
    for (Integer bidderId : bidderIds) {
      userDAO.lockRow(conn, bidderId);
      User user =
          userDAO
              .findById(conn, bidderId)
              .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + bidderId));
      user.getWallet().releaseFrozen(String.valueOf(auction.getId()));
      userDAO.update(conn, user);
    }
  }

  private Set<Integer> bidderIds(java.sql.Connection conn, int auctionId) {
    List<Bid> bids = bidDAO.findByAuction(conn, auctionId);
    Set<Integer> bidderIds = new LinkedHashSet<>();
    for (Bid bid : bids) {
      bidderIds.add(bid.getBidderId());
    }
    return bidderIds;
  }
}
