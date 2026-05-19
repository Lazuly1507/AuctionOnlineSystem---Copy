package app.server.dao.impl;

import app.common.exception.DatabaseException;
import app.common.models.AutoBid;
import app.server.dao.AutoBidDAO;
import app.server.dao.BaseDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** MySqlAutoBidDAO. */
public class MySqlAutoBidDAO extends BaseDAO implements AutoBidDAO {
  private static final String TABLE = "auto_bids";
  private static final String BASE_SELECT =
      """
      SELECT
          id,
          session_id,
          user_id,
          max_amount,
          increment_amount,
          enabled,
          created_at,
          updated_at
      FROM auto_bids
      """;

  private AutoBid mapAutoBid(ResultSet rs) throws SQLException {
    return new AutoBid(
        rs.getInt("id"),
        rs.getInt("session_id"),
        rs.getInt("user_id"),
        rs.getLong("max_amount"),
        rs.getLong("increment_amount"),
        rs.getBoolean("enabled"),
        rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getTimestamp("updated_at").toLocalDateTime());
  }

  @Override
  public Optional<AutoBid> findById(int id) {
    return withConnection(
        conn -> findOne(conn, BASE_SELECT + " WHERE id = ?", id), "Lỗi kết nối khi tải auto bid.");
  }

  @Override
  public Optional<AutoBid> findByAuctionAndUser(int auctionId, int userId) {
    return withConnection(
        conn -> findByAuctionAndUser(conn, auctionId, userId), "Lỗi kết nối khi tải auto bid.");
  }

  @Override
  public Optional<AutoBid> findByAuctionAndUser(Connection conn, int auctionId, int userId) {
    return findOne(conn, BASE_SELECT + " WHERE session_id = ? AND user_id = ?", auctionId, userId);
  }

  @Override
  public List<AutoBid> findByAuction(int auctionId) {
    return withConnection(
        conn ->
            findMany(
                conn, BASE_SELECT + " WHERE session_id = ? ORDER BY max_amount DESC", auctionId),
        "Lỗi kết nối khi tải danh sách auto bid.");
  }

  @Override
  public List<AutoBid> findEnabledByAuction(int auctionId) {
    return withConnection(
        conn -> findEnabledByAuction(conn, auctionId),
        "Lỗi kết nối khi tải auto bid đang hoạt động.");
  }

  @Override
  public List<AutoBid> findEnabledByAuction(Connection conn, int auctionId) {
    return findMany(
        conn,
        BASE_SELECT
            + """
              WHERE session_id = ?
              AND enabled = TRUE
              ORDER BY max_amount DESC
              """,
        auctionId);
  }

  @Override
  public AutoBid save(AutoBid autoBid) {
    return withConnection(conn -> save(conn, autoBid), "Lỗi kết nối khi tạo auto bid.");
  }

  @Override
  public AutoBid save(Connection conn, AutoBid autoBid) {
    String sql =
        """
        INSERT INTO auto_bids
            (session_id, user_id, max_amount, increment_amount, enabled)
        VALUES (?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      setParameters(
          ps,
          autoBid.getAuctionId(),
          autoBid.getUserId(),
          autoBid.getMaxAmount(),
          autoBid.getIncrementAmount(),
          autoBid.isEnabled());
      if (ps.executeUpdate() == 0) {
        throw new DatabaseException("Không thể tạo auto bid.");
      }
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          autoBid.setId(rs.getInt(1));
        }
      }
      return autoBid;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi tạo auto bid.", e);
    }
  }

  @Override
  public boolean update(AutoBid autoBid) {
    return withConnection(conn -> update(conn, autoBid), "Lỗi kết nối khi cập nhật auto bid.");
  }

  @Override
  public boolean update(Connection conn, AutoBid autoBid) {
    String sql =
        """
        UPDATE auto_bids
        SET
            max_amount = ?,
            increment_amount = ?,
            enabled = ?
        WHERE id = ?
        """;
    return executeUpdate(
        conn,
        sql,
        autoBid.getMaxAmount(),
        autoBid.getIncrementAmount(),
        autoBid.isEnabled(),
        autoBid.getId());
  }

  @Override
  public boolean delete(int id) {
    return withConnection(
        conn -> executeUpdate(conn, "DELETE FROM auto_bids WHERE id = ?", id),
        "Lỗi kết nối khi xóa auto bid.");
  }

  @Override
  public boolean setEnabled(int id, boolean enabled) {
    return withConnection(
        conn -> executeUpdate(conn, "UPDATE auto_bids SET enabled = ? WHERE id = ?", enabled, id),
        "Lỗi kết nối khi cập nhật trạng thái auto bid.");
  }

  private Optional<AutoBid> findOne(Connection conn, String sql, Object... params) {
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapAutoBid(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  private List<AutoBid> findMany(Connection conn, String sql, Object... params) {
    List<AutoBid> autoBids = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          autoBids.add(mapAutoBid(rs));
        }
      }
      return autoBids;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn danh sách auto bid.", e);
    }
  }
}
