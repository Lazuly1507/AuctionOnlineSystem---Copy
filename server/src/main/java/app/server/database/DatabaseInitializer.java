package app.server.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/** DatabaseInitializer. */
public class DatabaseInitializer {
  /** initialize. */
  public static void initialize() {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection();
        InputStream stream = DatabaseConnection.class.getResourceAsStream("/schema.sql")) {
      if (stream == null) {
        throw new IOException("schema.sql not found on classpath");
      }
      String schema = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      String[] statements = schema.split(";");
      for (String stmt : statements) {
        String sql = stmt.trim();
        if (sql.isEmpty()) {
          continue;
        }
        // Connection already points to DB_NAME, skip USE statements from schema files.
        if (sql.toUpperCase().startsWith("USE ")) {
          continue;
        }
        try (Statement statement = conn.createStatement()) {
          statement.execute(sql);
        }
      }
      System.out.println("Database initialization completed.");
    } catch (Exception e) {
      System.out.println("Database initialization failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  static void main() {
    DatabaseInitializer.initialize();
  }
}
