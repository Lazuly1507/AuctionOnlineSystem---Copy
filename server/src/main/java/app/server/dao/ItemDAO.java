package app.server.dao;

import app.common.enums.ItemType;
import app.common.models.Item;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** ItemDAO. */
public interface ItemDAO {
  /** findById. */
  Optional<Item> findById(int id);

  /** findById. */
  Optional<Item> findById(Connection conn, int id);

  /** findAll. */
  List<Item> findAll();

  /** findBySeller. */
  List<Item> findBySeller(int sellerId);

  /** findBySeller. */
  List<Item> findBySeller(Connection conn, int sellerId);

  /** findByCategory. */
  List<Item> findByCategory(ItemType type);

  /** findAvailable. */
  List<Item> findAvailable();

  /** save. */
  Item save(Item item);

  /** save. */
  Item save(Connection conn, Item item);

  /** update. */
  void update(Item item);

  /** update. */
  void update(Connection conn, Item item);
}
