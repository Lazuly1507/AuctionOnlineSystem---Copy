package app.server.dao;

import app.common.models.AutoBid;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** AutoBidDAO. */
public interface AutoBidDAO {
  /** findById. */
  Optional<AutoBid> findById(int id);

  /** findByAuctionAndUser. */
  Optional<AutoBid> findByAuctionAndUser(int auctionId, int userId);

  /** findByAuctionAndUser. */
  Optional<AutoBid> findByAuctionAndUser(Connection conn, int auctionId, int userId);

  /** findByAuction. */
  List<AutoBid> findByAuction(int auctionId);

  /** findEnabledByAuction. */
  List<AutoBid> findEnabledByAuction(int auctionId);

  /** findEnabledByAuction. */
  List<AutoBid> findEnabledByAuction(Connection conn, int auctionId);

  /** save. */
  AutoBid save(AutoBid autoBid);

  /** save. */
  AutoBid save(Connection conn, AutoBid autoBid);

  /** update. */
  boolean update(AutoBid autoBid);

  /** update. */
  boolean update(Connection conn, AutoBid autoBid);

  /** delete. */
  boolean delete(int id);

  /** setEnabled. */
  boolean setEnabled(int id, boolean enabled);
}
