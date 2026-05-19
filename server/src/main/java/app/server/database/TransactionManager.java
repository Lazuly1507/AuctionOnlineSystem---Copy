package app.server.database;

import app.common.exception.DatabaseException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

/** TransactionManager. */
public class TransactionManager {
  /** runInTransaction. */
  public <T> T runInTransaction(Function<Connection, T> work) {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
      conn.setAutoCommit(false);
      try {
        T result = work.apply(conn);
        conn.commit();
        return result;
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi transaction.", e);
    }
  }

  /** runWithoutResult. */
  public void runWithoutResult(Consumer<Connection> work) {
    runInTransaction(
        conn -> {
          work.accept(conn);
          return null;
        });
  }
}
