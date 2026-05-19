package app.client.command;

import app.client.store.AuctionStore;
import app.client.store.ItemStore;
import app.common.dto.AuctionDetail;
import app.common.dto.CreateAuctionResponse;
import app.common.mapper.DtoMapper;
import app.common.protocol.PacketRes;

/** CreateAuctionCommand. */
public class CreateAuctionCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      CreateAuctionResponse response = packet.getData(CreateAuctionResponse.class);
      if (response != null && response.detail() != null) {
        AuctionDetail detail = response.detail();
        AuctionStore.getInstance().addAuction(DtoMapper.toAuction(detail.auction()));
        ItemStore.getInstance().addItem(DtoMapper.toItem(detail.item()));
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
