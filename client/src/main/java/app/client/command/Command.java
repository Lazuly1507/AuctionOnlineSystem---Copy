package app.client.command;

import app.client.manager.ClientNotificationCenter;
import app.common.protocol.ServerPacket;

/** Command. */
public abstract class Command {
  /** execute. */
  public abstract void execute(ServerPacket packet);

  public void notifyMessage(String msg) {
    if (msg == null || msg.isBlank()) {
      return;
    }
    ClientNotificationCenter.getInstance().notifyMessage(msg);
  }

  public void notifyUpdate() {
    ClientNotificationCenter.getInstance().notifyUpdate();
  }

  public void notify(String msg) {
    notifyMessage(msg);
  }
}
