package app.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** DatabaseConnection. */
public final class DatabaseConnection {
  private static volatile HikariDataSource dataSource;

  static {
    init();
  }

  private DatabaseConnection() {}

  private static synchronized void init() {
    DatabaseConfig cfg = DatabaseConfig.load();
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(cfg.getUrl());
    config.setUsername(cfg.getUser());
    config.setPassword(cfg.getPassword());
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setIdleTimeout(600000);
    config.setConnectionTimeout(30000);
    config.setMaxLifetime(1800000);
    dataSource = new HikariDataSource(config);
  }

  /** resetDataSource. */
  public static synchronized void resetDataSource() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
    init();
  }

  public static HikariDataSource getDataSource() {
    return dataSource;
  }
}
