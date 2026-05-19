package app.server.dao;

import app.server.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseDAOTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseDAOTest.class);
  private static final String TEST_DB_URL =
      "jdbc:h2:mem:auction_db_test;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
  private static final String USER = "sa";
  private static final String PASS = "";

  // =========================
  // GLOBAL SETUP (RUN ONCE)
  // =========================
  @BeforeAll
  void globalSetup() {
    configureTestEnvironment();
    reloadConnectionPool();
    initSchema();
  }

  // =========================
  // CLEAN BEFORE EACH TEST
  // =========================
  @BeforeEach
  void cleanData() {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.executeUpdate("DELETE FROM auto_bids");
      stmt.executeUpdate("DELETE FROM bids");
      stmt.executeUpdate("DELETE FROM auction_sessions");
      stmt.executeUpdate("DELETE FROM items");
      stmt.executeUpdate("DELETE FROM users");
    } catch (Exception e) {
      throw new RuntimeException("Failed to clean test data", e);
    }
  }

  // =========================
  // CONFIG TEST ENV
  // =========================
  private void configureTestEnvironment() {
    logger.info("[CONFIG] Setting H2 test DB environment...");
    System.setProperty("db.url", TEST_DB_URL);
    System.setProperty("db.user", USER);
    System.setProperty("db.password", PASS);
  }

  // =========================
  // FIXED POOL HANDLING
  // =========================
  private void reloadConnectionPool() {
    logger.info("[POOL] Reloading Hikari pool safely...");
    DatabaseConnection.resetDataSource();
  }

  // =========================
  // INIT SCHEMA
  // =========================
  private void initSchema() {
    logger.info("[SCHEMA] Initializing schema...");
    String sql = loadSchemaFile();
    try (Connection conn = DatabaseConnection.getDataSource().getConnection();
        Statement stmt = conn.createStatement()) {
      executeSqlScript(stmt, sql);
      logger.info("[SCHEMA] OK");
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize schema", e);
    }
  }

  // =========================
  // LOAD SCHEMA
  // =========================
  private String loadSchemaFile() {
    try {
      return new String(
          Objects.requireNonNull(BaseDAOTest.class.getResourceAsStream("/schema.sql"))
              .readAllBytes());
    } catch (Exception e) {
      throw new RuntimeException("Cannot load schema.sql", e);
    }
  }

  // =========================
  // EXEC SQL SCRIPT
  // =========================
  private void executeSqlScript(Statement stmt, String sql) throws Exception {
    for (String statement : sql.split(";")) {
      String trimmed = statement.trim();
      if (!trimmed.isEmpty()) {
        stmt.execute(trimmed);
      }
    }
  }
}
