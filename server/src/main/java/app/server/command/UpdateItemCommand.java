package app.server.command;

import app.common.dto.ItemResponse;
import app.common.dto.UpdateItemRequest;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UpdateItemCommand. */
public class UpdateItemCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(UpdateItemCommand.class);
  private final ItemService itemService;

  /** UpdateItemCommand. */
  public UpdateItemCommand(ItemService itemService) {
    this.itemService = itemService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      UpdateItemRequest request = packet.getData(UpdateItemRequest.class);
      if (request == null) {
        sendError(clientHandler, "Dữ liệu sản phẩm không hợp lệ.");
        return;
      }
      User user = clientHandler.getUser();
      Item item =
          ItemFactory.createItem(
              request.itemId(),
              request.name(),
              user.getId(),
              request.description(),
              request.startingPrice(),
              request.stepPrice(),
              request.type());
      Item updated = itemService.updateManagedItem(item, user.getId(), user.getRole());
      clientHandler.sendPacket(
          PacketRes.of(
              PacketType.UPDATE_ITEM,
              "Cập nhật sản phẩm thành công.",
              new ItemResponse(DtoMapper.toItemData(updated))));
    } catch (ServiceException e) {
      logger.warn("Update item failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected update item error", e);
      sendError(clientHandler, "Không thể cập nhật sản phẩm.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.UPDATE_ITEM, message));
  }
}
