package app.server.dao;

import app.common.models.Bid;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** BidDAO. */
public interface BidDAO {
  /** insertBid. */
  void insertBid(int auctionId, int userId, long bidAmount, boolean isAutoBid);

  /** insertBid. */
  void insertBid(Connection conn, int auctionId, int userId, long bidAmount, boolean isAutoBid);

  /** findHighestBid. */
  Optional<Bid> findHighestBid(int auctionId);

  /** findHighestBid. */
  Optional<Bid> findHighestBid(Connection conn, int auctionId);

  /** findByAuction. */
  List<Bid> findByAuction(int auctionId);

  /** findByAuction. */
  List<Bid> findByAuction(Connection conn, int auctionId);

  /** findByAuctionOrderByTime. */
  List<Bid> findByAuctionOrderByTime(int auctionId);

  /** existsByAuctionAndUser. */
  boolean existsByAuctionAndUser(int auctionId, int userId);
}
