package app.client.command;

import app.client.store.AuctionStore;
import app.client.store.ItemStore;
import app.common.dto.AuctionDetailResponse;
import app.common.mapper.DtoMapper;
import app.common.protocol.ServerPacket;

/** FetchAuctionDetailCommand. */
public class FetchAuctionDetailCommand extends Command {
  @Override
  public void execute(ServerPacket packet) {
    if (packet.isSuccess()) {
      AuctionDetailResponse response = packet.getData(AuctionDetailResponse.class);
      if (response != null && response.detail() != null) {
        AuctionStore.getInstance().addAuction(DtoMapper.toAuction(response.detail().auction()));
        ItemStore.getInstance().addItem(DtoMapper.toItem(response.detail().item()));
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
