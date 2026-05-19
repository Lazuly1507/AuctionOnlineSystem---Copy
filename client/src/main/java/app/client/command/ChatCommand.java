package app.client.command;

import app.common.dto.ChatResponse;
import app.common.protocol.PacketRes;

/** ChatCommand. */
public class ChatCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet == null) {
      return;
    }
    ChatResponse response = packet.getData(ChatResponse.class);
    if (response == null) {
      notifyMessage(packet.getMessage());
      return;
    }
    notifyMessage(response.sender() + ": " + response.content());
  }
}
