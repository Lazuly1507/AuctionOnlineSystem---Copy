package app.server.command;

import app.common.dto.AuctionHistoryRequest;
import app.common.dto.AuctionHistoryResponse;
import app.common.enums.PacketType;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionHistoryCommand. */
public class FetchAuctionHistoryCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionHistoryCommand.class);
  private final AuctionService auctionService;

  /** FetchAuctionHistoryCommand. */
  public FetchAuctionHistoryCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      int userId = clientHandler.getUser().getId();
      AuctionHistoryRequest request = packet.getData(AuctionHistoryRequest.class);
      int sinceVersion = request == null ? -1 : request.sinceVersion();
      var summaries = auctionService.getHistorySummaries(userId);
      boolean fullSnapshot = sinceVersion < 0;
      if (!fullSnapshot) {
        summaries = summaries.stream().filter(summary -> summary.version() > sinceVersion).toList();
      }
      AuctionHistoryResponse response = new AuctionHistoryResponse(summaries, fullSnapshot);
      clientHandler.sendPacket(PacketRes.of(PacketType.FETCH_AUCTION_HISTORY, "OK", response));
    } catch (Exception e) {
      logger.error("Failed to fetch auction history", e);
      clientHandler.sendPacket(
          PacketRes.error(PacketType.FETCH_AUCTION_HISTORY, "Không thể tải lịch sử đấu giá"));
    }
  }
}
