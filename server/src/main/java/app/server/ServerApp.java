package app.server;

import app.server.network.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** ServerApp. */
public class ServerApp {
  private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

  public static void main(String[] args) {
    Server server = Server.getInstance();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("Received shutdown signal");
                  server.shutdown();
                }));
    server.start();
  }
}
