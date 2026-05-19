package app.client.command;

import app.client.store.ItemStore;
import app.common.dto.ItemResponse;
import app.common.mapper.DtoMapper;
import app.common.protocol.PacketRes;

/** DeleteItemCommand. */
public class DeleteItemCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      ItemResponse response = packet.getData(ItemResponse.class);
      if (response != null && response.item() != null) {
        ItemStore.getInstance().addItem(DtoMapper.toItem(response.item()));
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
