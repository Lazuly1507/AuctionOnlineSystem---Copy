package app.client.command;

import app.common.dto.RegisterResponse;
import app.common.protocol.ServerPacket;

/** RegisterCommand. */
public class RegisterCommand extends Command {
  @Override
  public void execute(ServerPacket packet) {
    if (packet.isSuccess()) {
      packet.getData(RegisterResponse.class);
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
