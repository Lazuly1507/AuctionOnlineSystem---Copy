package app.client.command;

import app.client.manager.ClientNotificationCenter;
import app.common.protocol.PacketRes;

/** Command. */
public abstract class Command {
  /** execute. */
  public abstract void execute(PacketRes packet);

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
