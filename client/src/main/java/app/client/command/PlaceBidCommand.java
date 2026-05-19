package app.client.command;

import app.client.store.AuctionStore;
import app.common.dto.PlaceBidResponse;
import app.common.protocol.PacketRes;

/** PlaceBidCommand. */
public class PlaceBidCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      PlaceBidResponse response = packet.getData(PlaceBidResponse.class);
      if (response != null) {
        AuctionStore.getInstance()
            .updateBid(response.auctionId(), response.highestBidAmount(), response.bidderId());
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
