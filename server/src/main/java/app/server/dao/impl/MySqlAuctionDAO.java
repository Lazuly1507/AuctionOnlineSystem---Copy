package app.server.dao.impl;

import app.common.enums.AuctionStatus;
import app.common.exception.DatabaseException;
import app.common.models.Auction;
import app.server.dao.AuctionDAO;
import app.server.dao.BaseDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MySqlAuctionDAO. */
public class MySqlAuctionDAO extends BaseDAO implements AuctionDAO {
  private static final Logger logger = LoggerFactory.getLogger(MySqlAuctionDAO.class);
  private static final String TABLE = "auction_sessions";
  private static final String BASE_SELECT =
      """
              SELECT
                  s.id,
                  s.status,
                  s.start_time,
                  s.end_time,
                  s.highest_bid,
                  s.extended_count,
                  s.version,
                  s.created_at,
                  s.updated_at,
                  i.id  AS item_id,
                  u.id  AS seller_id,
                  w.id  AS winner_id
              FROM auction_sessions s
              JOIN items i ON s.item_id = i.id
              JOIN users u ON s.seller_id = u.id
              LEFT JOIN users w ON s.winner_id = w.id
              """;

  /** MySqlAuctionDAO. */
  public MySqlAuctionDAO() {}

  private Auction mapAuction(ResultSet rs) throws SQLException {
    return new Auction(
        rs.getInt("id"),
        rs.getInt("item_id"),
        rs.getInt("seller_id"),
        (Integer) rs.getObject("winner_id"),
        AuctionStatus.valueOf(rs.getString("status")),
        Optional.ofNullable(rs.getTimestamp("start_time"))
            .map(Timestamp::toLocalDateTime)
            .orElse(null),
        Optional.ofNullable(rs.getTimestamp("end_time"))
            .map(Timestamp::toLocalDateTime)
            .orElse(null),
        rs.getLong("highest_bid"),
        rs.getInt("extended_count"),
        rs.getInt("version"),
        rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getTimestamp("updated_at").toLocalDateTime());
  }

  @Override
  public Optional<Auction> findById(int id) {
    return withConnection(conn -> findById(conn, id), "Lỗi kết nối khi tải phiên đấu giá.");
  }

  @Override
  public Optional<Auction> findById(Connection conn, int id) {
    return findOne(conn, BASE_SELECT + " WHERE s.id = ?", id);
  }

  @Override
  public List<Auction> findAll() {
    return withConnection(
        conn -> findMany(conn, BASE_SELECT + " ORDER BY s.id DESC"),
        "Lỗi kết nối khi tải danh sách phiên đấu giá.");
  }

  @Override
  public List<Auction> findByStatus(AuctionStatus status) {
    return withConnection(
        conn ->
            findMany(
                conn, BASE_SELECT + " WHERE s.status = ? ORDER BY s.end_time ASC", status.name()),
        "Lỗi kết nối khi tải danh sách phiên theo trạng thái.");
  }

  @Override
  public List<Auction> findBySeller(int sellerId) {
    return withConnection(
        conn -> findMany(conn, BASE_SELECT + " WHERE s.seller_id = ? ORDER BY s.id DESC", sellerId),
        "Lỗi kết nối khi tải danh sách phiên của người bán.");
  }

  @Override
  public List<Auction> findByItemId(Connection conn, int itemId) {
    return findMany(conn, BASE_SELECT + " WHERE s.item_id = ? ORDER BY s.id DESC", itemId);
  }

  @Override
  public void lockRow(Connection conn, int auctionId) {
    String sql = "SELECT id FROM auction_sessions WHERE id = ? FOR UPDATE";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          throw new DatabaseException("Phiên đấu giá không tồn tại.");
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi khóa phiên đấu giá.", e);
    }
  }

  @Override
  public Auction save(Auction auction) {
    return withConnection(conn -> save(conn, auction), "Lỗi kết nối khi tạo auction.");
  }

  @Override
  public Auction save(Connection conn, Auction auction) {
    String sql =
        """
            INSERT INTO auction_sessions
                (item_id, seller_id, status, end_time, highest_bid)
            VALUES (?, ?, ?, ?, ?)
            """;
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      setParameters(
          ps,
          auction.getItemId(),
          auction.getSellerId(),
          auction.getStatus().name(),
          Timestamp.valueOf(auction.getEndTime()),
          auction.getHighestBid());
      if (ps.executeUpdate() == 0) {
        throw new DatabaseException("Không thể tạo auction.");
      }
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          auction.setId(rs.getInt(1));
        }
      }
      return auction;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi tạo auction.", e);
    }
  }

  @Override
  public boolean update(Auction auction) {
    return withConnection(conn -> update(conn, auction), "Lỗi kết nối khi cập nhật phiên đấu giá.");
  }

  @Override
  public boolean update(Connection conn, Auction auction) {
    String sql =
        "UPDATE auction_sessions SET status = ?, start_time = ?, end_time = ?, highest_bid = ?, "
            + "extended_count = ?, winner_id = ?, version = version + 1 WHERE id = ?";
    logger.info(
        "[DAO] Updating auction without version check: auctionId={}, status={}, currentVersion={}",
        auction.getId(),
        auction.getStatus(),
        auction.getVersion());
    boolean updated = updateAuctionRow(conn, auction, sql, auction.getId());
    if (updated) {
      auction.incrementVersion();
    }
    logger.info(
        "[DAO] Update without version check result: auctionId={}, updated={}, newVersion={}",
        auction.getId(),
        updated,
        auction.getVersion());
    return updated;
  }

  @Override
  public boolean updateIfVersionMatches(Connection conn, Auction auction, int expectedVersion) {
    String sql =
        "UPDATE auction_sessions SET status = ?, start_time = ?, end_time = ?, highest_bid = ?, "
            + "extended_count = ?, winner_id = ?, version = version + 1 "
            + "WHERE id = ? AND version = ?";
    logger.info(
        "[DAO] Reviewing auction with version check: auctionId={}, status={}, "
            + "expectedVersion={}, modelVersion={}",
        auction.getId(),
        auction.getStatus(),
        expectedVersion,
        auction.getVersion());
    boolean updated = updateAuctionRow(conn, auction, sql, auction.getId(), expectedVersion);
    if (updated) {
      auction.incrementVersion();
    }
    logger.info(
        "[DAO] Version check result: auctionId={}, matched={}, expectedVersion={}, newVersion={}",
        auction.getId(),
        updated,
        expectedVersion,
        auction.getVersion());
    return updated;
  }

  private boolean updateAuctionRow(
      Connection conn, Auction auction, String sql, Object... whereParams) {
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auction.getStatus().name());
      if (auction.getStartTime() == null) {
        ps.setNull(2, Types.TIMESTAMP);
      } else {
        ps.setTimestamp(2, Timestamp.valueOf(auction.getStartTime()));
      }
      if (auction.getEndTime() == null) {
        ps.setNull(3, Types.TIMESTAMP);
      } else {
        ps.setTimestamp(3, Timestamp.valueOf(auction.getEndTime()));
      }
      ps.setLong(4, auction.getHighestBid());
      ps.setInt(5, auction.getExtendedCount());
      ps.setObject(6, auction.getWinnerId());
      for (int i = 0; i < whereParams.length; i++) {
        ps.setObject(7 + i, whereParams[i]);
      }
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi cập nhật phiên đấu giá.", e);
    }
  }

  @Override
  public boolean delete(int id) {
    return withConnection(
        conn -> executeUpdate(conn, "DELETE FROM auction_sessions WHERE id = ?", id),
        "Lỗi kết nối khi xóa phiên đấu giá.");
  }

  private Optional<Auction> findOne(Connection conn, String sql, Object... params) {
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapAuction(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  private List<Auction> findMany(Connection conn, String sql, Object... params) {
    List<Auction> auctions = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          auctions.add(mapAuction(rs));
        }
      }
      return auctions;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn danh sách auctions.", e);
    }
  }
}
