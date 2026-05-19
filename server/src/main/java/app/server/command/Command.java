package app.server.command;

import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;

/** Command. */
public abstract class Command {
  /** execute. */
  public abstract void execute(ClientHandler clientHandler, PacketReq packet);
}
