package app.server.command;

import app.common.dto.DeleteItemRequest;
import app.common.dto.ItemResponse;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.Item;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DeleteItemCommand. */
public class DeleteItemCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(DeleteItemCommand.class);
  private final ItemService itemService;

  /** DeleteItemCommand. */
  public DeleteItemCommand(ItemService itemService) {
    this.itemService = itemService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      DeleteItemRequest request = packet.getData(DeleteItemRequest.class);
      if (request == null || request.itemId() <= 0) {
        sendError(clientHandler, "Dữ liệu sản phẩm không hợp lệ.");
        return;
      }
      User user = clientHandler.getUser();
      Item deleted =
          itemService.softDeleteManagedItem(request.itemId(), user.getId(), user.getRole());
      clientHandler.sendPacket(
          PacketRes.of(
              true,
              PacketType.DELETE_ITEM,
              "Xóa sản phẩm thành công.",
              new ItemResponse(DtoMapper.toItemData(deleted))));
    } catch (ServiceException e) {
      logger.warn("Delete item failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected delete item error", e);
      sendError(clientHandler, "Không thể xóa sản phẩm.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.DELETE_ITEM, message));
  }
}
