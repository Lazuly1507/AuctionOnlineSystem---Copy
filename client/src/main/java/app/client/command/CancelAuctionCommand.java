package app.client.command;

import app.client.store.AuctionStore;
import app.common.dto.CancelAuctionResponse;
import app.common.protocol.PacketRes;

/** CancelAuctionCommand. */
public class CancelAuctionCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      CancelAuctionResponse response = packet.getData(CancelAuctionResponse.class);
      if (response != null && response.auctionId() > 0) {
        AuctionStore.getInstance().markCanceled(response.auctionId());
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
