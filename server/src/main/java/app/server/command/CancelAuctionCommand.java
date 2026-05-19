package app.server.command;

import app.common.dto.CancelAuctionRequest;
import app.common.dto.CancelAuctionResponse;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CancelAuctionCommand. */
public class CancelAuctionCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(CancelAuctionCommand.class);
  private final AuctionService auctionService;

  /** CancelAuctionCommand. */
  public CancelAuctionCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    int auctionId = 0;
    try {
      CancelAuctionRequest request = packet.getData(CancelAuctionRequest.class);
      if (request == null || request.auctionId() <= 0 || request.expectedVersion() < 0) {
        sendError(clientHandler, auctionId, "Dữ liệu phiên đấu giá không hợp lệ.");
        return;
      }
      auctionId = request.auctionId();
      auctionService.cancelAuction(
          auctionId, clientHandler.getUser().getId(), request.expectedVersion());
      clientHandler.sendPacket(
          PacketRes.of(
              PacketType.CANCEL_AUCTION,
              "Hủy phiên thành công.",
              new CancelAuctionResponse(auctionId)));
      Server.broadcast(
          PacketRes.of(
              PacketType.AUCTION_CANCELLED,
              "Phiên đã bị hủy.",
              new CancelAuctionResponse(auctionId)),
          clientHandler.getUser().getId());
    } catch (ServiceException e) {
      logger.warn("Cancel auction failed: {}", e.getMessage());
      sendError(clientHandler, auctionId, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected cancel auction error", e);
      sendError(clientHandler, auctionId, "Không thể hủy phiên đấu giá.");
    }
  }

  private void sendError(ClientHandler clientHandler, int auctionId, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.CANCEL_AUCTION, message));
  }
}
