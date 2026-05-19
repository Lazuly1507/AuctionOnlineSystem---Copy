package app.server.dao;

import app.common.enums.AuctionStatus;
import app.common.models.Auction;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Auction operations.
 *
 * <p>Methods are categorized as: 1. Read-only operations: Create own connection (no Connection
 * param) 2. Transaction operations: Accept Connection parameter for participation in transactions
 */
public interface AuctionDAO {
  /** findById. */
  Optional<Auction> findById(int id);

  /** findById. */
  Optional<Auction> findById(Connection conn, int id);

  /** findAll. */
  List<Auction> findAll();

  /** findByStatus. */
  List<Auction> findByStatus(AuctionStatus status);

  /** findBySeller. */
  List<Auction> findBySeller(int sellerId);

  /** findByItemId. */
  List<Auction> findByItemId(Connection conn, int itemId);

  /** save. */
  Auction save(Auction auction);

  /** save. */
  Auction save(Connection conn, Auction auction);

  /** update. */
  boolean update(Auction auction);

  /** update. */
  boolean update(Connection conn, Auction auction);

  /** delete. */
  boolean delete(int id);

  /** lockRow. */
  void lockRow(Connection conn, int auctionId);

  /** updateIfVersionMatches. */
  boolean updateIfVersionMatches(Connection conn, Auction auction, int expectedVersion);
}
