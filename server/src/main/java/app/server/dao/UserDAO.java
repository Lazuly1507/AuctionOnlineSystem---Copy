package app.server.dao;

import app.common.models.User;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** UserDAO. */
public interface UserDAO {
  /** findById. */
  Optional<User> findById(int id);

  /** findById. */
  Optional<User> findById(Connection conn, int id);

  /** findByUsername. */
  Optional<User> findByUsername(String username);

  /** findByUsername. */
  Optional<User> findByUsername(Connection conn, String username);

  /** findAll. */
  List<User> findAll();

  /** save. */
  User save(User user);

  /** save. */
  User save(Connection conn, User user);

  /** update. */
  void update(User user);

  /** update. */
  void update(Connection conn, User user);

  /** lockRow. */
  void lockRow(Connection conn, int id);

  /** deleteAll. */
  void deleteAll();
}
