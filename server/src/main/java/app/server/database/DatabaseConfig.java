package app.server.database;

/** DatabaseConfig. */
public final class DatabaseConfig {
  private final String url;
  private final String user;
  private final String password;

  private DatabaseConfig(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  /** load. */
  public static DatabaseConfig load() {
    String url = System.getProperty("db.url", "jdbc:mysql://localhost:3306/auction_db");
    String user = System.getProperty("db.user", "root");
    String pass = System.getProperty("db.password", "123456");
    return new DatabaseConfig(url, user, pass);
  }

  public String getUrl() {
    return url;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }
}
