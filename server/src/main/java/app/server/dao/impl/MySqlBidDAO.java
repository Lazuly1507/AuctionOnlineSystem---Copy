package app.server.dao.impl;

import app.common.exception.DatabaseException;
import app.common.models.Bid;
import app.server.dao.BaseDAO;
import app.server.dao.BidDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** MySqlBidDAO. */
public class MySqlBidDAO extends BaseDAO implements BidDAO {
  /** MySqlBidDAO. */
  public MySqlBidDAO() {}

  private static final String BID_SELECT =
      """
          SELECT
              b.id,
              b.session_id,
              b.bid_amount,
              b.bid_time,
              b.is_auto_bid,
              u.id   AS user_id,
              u.full_name
          FROM bids b
          JOIN users u ON b.user_id = u.id
          WHERE b.session_id = ?
          """;

  private Bid mapBid(ResultSet rs) throws SQLException {
    return new Bid(
        rs.getInt("id"),
        rs.getInt("session_id"),
        rs.getInt("user_id"),
        rs.getString("full_name"),
        rs.getLong("bid_amount"),
        rs.getTimestamp("bid_time").toLocalDateTime(),
        rs.getBoolean("is_auto_bid"));
  }

  @Override
  public void insertBid(int auctionId, int userId, long bidAmount, boolean isAutoBid)
      throws DatabaseException {
    runWithConnection(
        conn -> insertBid(conn, auctionId, userId, bidAmount, isAutoBid),
        "Lỗi kết nối khi thêm bid.");
  }

  @Override
  public void insertBid(
      Connection conn, int auctionId, int userId, long bidAmount, boolean isAutoBid) {
    String sql =
        """
            INSERT INTO bids (session_id, user_id, bid_amount, is_auto_bid)
            VALUES (?, ?, ?, ?)
            """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, auctionId, userId, bidAmount, isAutoBid);
      if (ps.executeUpdate() == 0) {
        throw new DatabaseException("Không thể thêm bid.");
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi thêm bid.", e);
    }
  }

  @Override
  public Optional<Bid> findHighestBid(int auctionId) {
    return withConnection(
        conn -> findHighestBid(conn, auctionId), "Lỗi kết nối khi truy vấn bid cao nhất.");
  }

  @Override
  public Optional<Bid> findHighestBid(Connection conn, int auctionId) {
    String sql = BID_SELECT + " ORDER BY b.bid_amount DESC, b.bid_time DESC LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapBid(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi truy vấn bid cao nhất.", e);
    }
  }

  @Override
  public List<Bid> findByAuction(int auctionId) {
    return withConnection(conn -> findByAuction(conn, auctionId), "Lỗi kết nối khi truy vấn bids.");
  }

  @Override
  public List<Bid> findByAuction(Connection conn, int auctionId) {
    return queryBids(conn, BID_SELECT + " ORDER BY b.bid_amount DESC, b.bid_time DESC", auctionId);
  }

  @Override
  public List<Bid> findByAuctionOrderByTime(int auctionId) {
    return withConnection(
        conn -> queryBids(conn, BID_SELECT + " ORDER BY b.bid_time ASC", auctionId),
        "Lỗi kết nối khi truy vấn bids.");
  }

  @Override
  public boolean existsByAuctionAndUser(int auctionId, int userId) {
    return withConnection(
        conn -> existsByAuctionAndUser(conn, auctionId, userId),
        "Lỗi kết nối khi kiểm tra tồn tại bid.");
  }

  private boolean existsByAuctionAndUser(Connection conn, int auctionId, int userId) {
    String sql = "SELECT 1 FROM bids WHERE session_id = ? AND user_id = ? LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, auctionId, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi kiểm tra tồn tại bid.", e);
    }
  }

  private List<Bid> queryBids(Connection conn, String sql, int auctionId) {
    List<Bid> bids = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          bids.add(mapBid(rs));
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi truy vấn bids.", e);
    }
    return bids;
  }
}
