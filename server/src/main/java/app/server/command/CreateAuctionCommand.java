package app.server.command;

import app.common.dto.*;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CreateAuctionCommand. */
public class CreateAuctionCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(CreateAuctionCommand.class);
  private final AuctionService auctionService;

  /** CreateAuctionCommand. */
  public CreateAuctionCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        clientHandler.sendPacket(
            PacketRes.error(PacketType.CREATE_AUCTION, "Authentication required"));
        return;
      }
      CreateAuctionRequest request = packet.getData(CreateAuctionRequest.class);
      if (request == null) {
        sendError(clientHandler, "Invalid request");
        return;
      }
      User user = clientHandler.getUser();
      Auction auction =
          auctionService.createAndStartAuctionWithItem(
              request.name(),
              request.description(),
              request.startingPrice(),
              request.stepPrice(),
              request.type(),
              request.durationMinutes(),
              user.getId(),
              user.getRole());
      var createdSnapshot = auctionService.getAuction(auction.getId());
      CreateAuctionResponse response =
          new CreateAuctionResponse(
              DtoMapper.toAuctionDetail(createdSnapshot.auction(), createdSnapshot.item()));
      PacketRes packetRes =
          PacketRes.of(PacketType.CREATE_AUCTION, "Tạo phiên thành công", response);
      clientHandler.sendPacket(packetRes);
      Server.broadcast(
          PacketRes.of(PacketType.AUCTION_CREATED, "Có phiên đấu giá mới.", response),
          user.getId());
      broadcastAuctionList();
      AuctionHistoryResponse historyResponse =
          new AuctionHistoryResponse(
              auctionService.getHistoryAuctions(clientHandler.getUser().getId()).stream()
                  .map(snapshot -> DtoMapper.toAuctionSummary(snapshot.auction(), snapshot.item()))
                  .toList());
      clientHandler.sendPacket(
          PacketRes.of(PacketType.FETCH_AUCTION_HISTORY, "OK", historyResponse));

      logger.info("Auction created successfully by user {}", user.getId());
    } catch (ServiceException e) {
      logger.warn("Create auction failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Create auction failed", e);
      sendError(clientHandler, "Tạo phiên thất bại");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.CREATE_AUCTION, message));
  }

  private void broadcastAuctionList() {
    try {
      AuctionSummariesResponse summariesResponse =
          new AuctionSummariesResponse(
              auctionService.getAuctions().stream()
                  .map(snapshot -> DtoMapper.toAuctionSummary(snapshot.auction(), snapshot.item()))
                  .toList());
      Server.broadcast(
          PacketRes.of(PacketType.AUCTION_SUMMARIES_UPDATED, "OK", summariesResponse), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction list", e);
    }
  }
}
