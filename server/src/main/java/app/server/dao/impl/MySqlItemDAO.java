package app.server.dao.impl;

import app.common.enums.ItemType;
import app.common.exception.DatabaseException;
import app.common.models.Item;
import app.common.models.ItemFactory;
import app.server.dao.BaseDAO;
import app.server.dao.ItemDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** MySqlItemDAO. */
public class MySqlItemDAO extends BaseDAO implements ItemDAO {
  /** MySqlItemDAO. */
  public MySqlItemDAO() {}

  private static final String TABLE = "items";
  private static final String BASE_SELECT =
      """
          SELECT id, name, seller_id, description, category,
                 starting_price, step_price, deleted
          FROM items
          """;

  private Item mapItem(ResultSet rs) throws SQLException {
    Item item =
        ItemFactory.createItem(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getInt("seller_id"),
            rs.getString("description"),
            rs.getLong("starting_price"),
            rs.getLong("step_price"),
            ItemType.valueOf(rs.getString("category")));
    item.setDeleted(rs.getBoolean("deleted"));
    return item;
  }

  @Override
  public Optional<Item> findById(int id) {
    return withConnection(conn -> findById(conn, id), "Lỗi kết nối khi tải item.");
  }

  @Override
  public Optional<Item> findById(Connection conn, int id) {
    return findOne(conn, BASE_SELECT + " WHERE id = ?", id);
  }

  @Override
  public List<Item> findAll() {
    return withConnection(
        conn -> findList(conn, BASE_SELECT + "ORDER BY id DESC"),
        "Lỗi kết nối khi tải danh sách items.");
  }

  @Override
  public List<Item> findBySeller(int sellerId) {
    return withConnection(
        conn -> findBySeller(conn, sellerId), "Lỗi kết nối khi tải danh sách item theo seller.");
  }

  @Override
  public List<Item> findBySeller(Connection conn, int sellerId) {
    return findList(conn, BASE_SELECT + "WHERE seller_id = ? ORDER BY id DESC", sellerId);
  }

  @Override
  public List<Item> findByCategory(ItemType type) {
    return withConnection(
        conn -> findList(conn, BASE_SELECT + "WHERE category = ? ORDER BY id DESC", type.name()),
        "Lỗi kết nối khi tải danh sách item theo category.");
  }

  @Override
  public List<Item> findAvailable() {
    return withConnection(
        conn -> findList(conn, BASE_SELECT + "WHERE deleted = FALSE ORDER BY id DESC"),
        "Lỗi kết nối khi tải danh sách item khả dụng.");
  }

  @Override
  public Item save(Item item) {
    return withConnection(conn -> save(conn, item), "Lỗi kết nối khi thêm item.");
  }

  @Override
  public Item save(Connection conn, Item item) {
    String sql =
        """
        INSERT INTO items (
            seller_id, name, description, category, starting_price, step_price
        )
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      setParameters(
          ps,
          item.getSellerId(),
          item.getName(),
          item.getDescription(),
          item.getType().name(),
          item.getStartingPrice(),
          item.getStepPrice());
      if (ps.executeUpdate() == 0) {
        throw new DatabaseException("Không thể thêm item.");
      }
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          item.setId(rs.getInt(1));
        }
      }
      return item;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi thêm item.", e);
    }
  }

  @Override
  public void update(Item item) {
    runWithConnection(conn -> update(conn, item), "Lỗi kết nối khi cập nhật item.");
  }

  @Override
  public void update(Connection conn, Item item) {
    String sql =
        """
        UPDATE items
        SET name = ?, description = ?, starting_price = ?, step_price = ?, category = ?,
            deleted = ?
        WHERE id = ?
        """;
    executeUpdate(
        conn,
        sql,
        item.getName(),
        item.getDescription(),
        item.getStartingPrice(),
        item.getStepPrice(),
        item.getType().name(),
        item.isDeleted(),
        item.getId());
  }

  private List<Item> findList(Connection conn, String sql, Object... params) {
    List<Item> items = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          items.add(mapItem(rs));
        }
      }
      return items;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  private Optional<Item> findOne(Connection conn, String sql, Object... params) {
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapItem(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }
}
