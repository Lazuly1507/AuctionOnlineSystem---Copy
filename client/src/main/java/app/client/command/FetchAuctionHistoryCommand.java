package app.client.command;

import app.client.store.AuctionStore;
import app.common.dto.AuctionHistoryResponse;
import app.common.protocol.PacketRes;

/** FetchAuctionHistoryCommand. */
public class FetchAuctionHistoryCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      AuctionHistoryResponse response = packet.getData(AuctionHistoryResponse.class);
      AuctionStore.getInstance().setHistorySummaries(response == null ? null : response.auctions());
      notifyUpdate();
    }
    if (packet != null && !packet.isSuccess()) {
      notifyMessage(packet.getMessage());
    }
  }
}
