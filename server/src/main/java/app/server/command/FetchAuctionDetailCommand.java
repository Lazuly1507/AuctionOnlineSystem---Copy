package app.server.command;

import app.common.dto.AuctionDetailRequest;
import app.common.dto.AuctionDetailResponse;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AuctionService;
import app.server.service.AuctionSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionDetailCommand. */
public class FetchAuctionDetailCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionDetailCommand.class);
  private final AuctionService auctionService;

  /** FetchAuctionDetailCommand. */
  public FetchAuctionDetailCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      AuctionDetailRequest request = packet.getData(AuctionDetailRequest.class);
      if (request == null) {
        clientHandler.sendPacket(
            PacketRes.error(PacketType.FETCH_AUCTION_DETAIL, "Invalid request"));
        return;
      }
      if (request.auctionId() <= 0) {
        clientHandler.sendPacket(
            PacketRes.error(PacketType.FETCH_AUCTION_DETAIL, "Invalid auction id"));
        return;
      }
      if (auctionService.isAuctionVersionCurrent(request.auctionId(), request.knownVersion())) {
        AuctionDetailResponse response =
            AuctionDetailResponse.notModified(request.auctionId(), request.knownVersion());
        clientHandler.sendPacket(PacketRes.of(PacketType.FETCH_AUCTION_DETAIL, "OK", response));
        return;
      }
      AuctionSnapshot auction = auctionService.getAuction(request.auctionId());
      AuctionDetailResponse response =
          new AuctionDetailResponse(DtoMapper.toAuctionDetail(auction.auction(), auction.item()));
      clientHandler.sendPacket(PacketRes.of(PacketType.FETCH_AUCTION_DETAIL, "OK", response));
    } catch (ServiceException e) {
      logger.warn("Fetch auction detail failed: {}", e.getMessage());
      clientHandler.sendPacket(PacketRes.error(PacketType.FETCH_AUCTION_DETAIL, e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error while fetching auction detail", e);
      clientHandler.sendPacket(
          PacketRes.error(PacketType.FETCH_AUCTION_DETAIL, "Internal server error"));
    }
  }
}
