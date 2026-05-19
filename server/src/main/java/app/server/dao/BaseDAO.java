package app.server.dao;

import app.common.exception.DatabaseException;
import app.server.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base DAO class providing common transaction and connection handling. Centralizes connection
 * management, error handling, and transaction patterns.
 */
public abstract class BaseDAO {
  /** executeUpdate. */
  protected boolean executeUpdate(Connection conn, String sql, Object... params) {
    validateUpdateSql(sql);
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi cập nhật bảng", e);
    }
  }

  private void validateUpdateSql(String sql) {
    if (sql == null || sql.isBlank()) {
      throw new DatabaseException("SQL cập nhật không được để trống.");
    }
    String normalized = sql.stripLeading().toUpperCase();
    if (!normalized.startsWith("INSERT")
        && !normalized.startsWith("UPDATE")
        && !normalized.startsWith("DELETE")) {
      throw new DatabaseException("SQL cập nhật không hợp lệ: " + sql);
    }
  }

  /** setParameters. */
  protected void setParameters(PreparedStatement ps, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) {
      ps.setObject(i + 1, params[i]);
    }
  }

  /** Execute action within a connection, auto-close when done. */
  protected <T> T withConnection(Function<Connection, T> action, String errorMessage) {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
      return action.apply(conn);
    } catch (SQLException e) {
      throw new DatabaseException(errorMessage, e);
    }
  }

  /** Execute action within a connection (void return), auto-close when done. */
  protected void runWithConnection(Consumer<Connection> action, String errorMessage) {
    withConnection(
        conn -> {
          action.accept(conn);
          return null;
        },
        errorMessage);
  }

  /**
   * Execute action within a transaction. Auto-commits on success, rollback on exception. Restores
   * original autoCommit state in finally block.
   */
  protected <T> T runInTransaction(Function<Connection, T> action, String errorMessage) {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
      boolean originalAutoCommit = conn.getAutoCommit();
      conn.setAutoCommit(false);
      try {
        T result = action.apply(conn);
        conn.commit();
        return result;
      } catch (RuntimeException e) {
        conn.rollback();
        throw e;
      } catch (SQLException e) {
        conn.rollback();
        throw new DatabaseException(errorMessage, e);
      } finally {
        conn.setAutoCommit(originalAutoCommit);
      }
    } catch (SQLException e) {
      throw new DatabaseException(errorMessage, e);
    }
  }

  /** Execute action within a transaction (void return). */
  protected void runInTransaction(Consumer<Connection> action, String errorMessage) {
    runInTransaction(
        conn -> {
          action.accept(conn);
          return null;
        },
        errorMessage);
  }
}
