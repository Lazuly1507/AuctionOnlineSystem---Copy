package app.server.command;

import app.common.dto.AuctionSummariesResponse;
import app.common.enums.PacketType;
import app.common.mapper.DtoMapper;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionSummariesCommand. */
public class FetchAuctionSummariesCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionSummariesCommand.class);
  private final AuctionService auctionService;

  /** FetchAuctionSummariesCommand. */
  public FetchAuctionSummariesCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      AuctionSummariesResponse response =
          new AuctionSummariesResponse(
              auctionService.getAuctions().stream()
                  .map(snapshot -> DtoMapper.toAuctionSummary(snapshot.auction(), snapshot.item()))
                  .toList());
      clientHandler.sendPacket(PacketRes.of(PacketType.FETCH_AUCTION_SUMMARIES, response));
    } catch (Exception e) {
      logger.error("Failed to fetch auctions", e);
      clientHandler.sendPacket(
          PacketRes.error(PacketType.FETCH_AUCTION_SUMMARIES, "Không thể tải danh sách đấu giá"));
    }
  }
}
