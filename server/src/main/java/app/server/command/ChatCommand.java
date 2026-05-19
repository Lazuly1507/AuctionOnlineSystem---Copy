package app.server.command;

import app.common.dto.ChatRequest;
import app.common.dto.ChatResponse;
import app.common.enums.PacketType;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** ChatCommand. */
public class ChatCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(ChatCommand.class);
  private static final int MAX_MESSAGE_LENGTH = 500;

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        clientHandler.sendPacket(PacketRes.error(PacketType.CHAT, "Authentication required"));
        return;
      }
      ChatRequest request = packet.getData(ChatRequest.class);
      if (request == null) {
        clientHandler.sendPacket(PacketRes.error(PacketType.CHAT, "Invalid request"));
        return;
      }
      String content = request.content();
      if (content == null || content.isBlank()) {
        clientHandler.sendPacket(PacketRes.error(PacketType.CHAT, "Message cannot be empty"));
        return;
      }
      content = content.trim();
      if (content.length() > MAX_MESSAGE_LENGTH) {
        clientHandler.sendPacket(PacketRes.error(PacketType.CHAT, "Message too long"));
        return;
      }
      User user = clientHandler.getUser();
      // KHÔNG trust sender từ client
      ChatResponse response =
          new ChatResponse(user.getId(), user.getName(), content, LocalDateTime.now());
      PacketRes chatPacket = PacketRes.of(PacketType.CHAT_MESSAGE, response);
      clientHandler.sendPacket(chatPacket);
      Server.broadcast(chatPacket, user.getId());
      logger.info("User {} sent chat message", user.getId());
    } catch (Exception e) {
      logger.error("Chat network failed", e);
      clientHandler.sendPacket(PacketRes.error(PacketType.CHAT, "Failed to send message"));
    }
  }
}
