package app.server.command;

import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.network.Session;

/** Clears current live-auction watcher context for a client session. */
public class UnwatchAuctionCommand extends Command {
  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    Session session = clientHandler.getSession();
    if (session == null) {
      return;
    }
    session.setViewingAuctionId(null);
  }
}
