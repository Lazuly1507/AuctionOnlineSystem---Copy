package app.client.command;

import app.client.store.AuctionStore;
import app.common.dto.AuctionResultResponse;
import app.common.protocol.PacketRes;

/** FetchAuctionResultCommand. */
public class FetchAuctionResultCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    String message = packet.getMessage();
    if (packet.isSuccess()) {
      AuctionResultResponse response = packet.getData(AuctionResultResponse.class);
      if (response != null) {
        Integer winnerId = response.winner() == null ? null : response.winner().id();
        AuctionStore.getInstance()
            .markFinished(response.auctionId(), response.finalPrice(), winnerId);
        String winnerName =
            response.winner() == null ? "chưa có người thắng" : response.winner().name();
        message =
            "Phiên đấu giá đã kết thúc. Người thắng: "
                + winnerName
                + " với giá: "
                + String.format("%,d đ", response.finalPrice());
      }
      notifyUpdate();
    }
    notifyMessage(message);
  }
}
