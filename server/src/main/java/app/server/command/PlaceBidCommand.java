package app.server.command;

import app.common.dto.*;
import app.common.enums.PacketType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionService;
import app.server.service.BidService;
import app.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** PlaceBidCommand. */
public class PlaceBidCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(PlaceBidCommand.class);
  private final BidService bidService;
  private final UserService userService;
  private final AuctionService auctionService;

  /** PlaceBidCommand. */
  public PlaceBidCommand(
      BidService bidService, UserService userService, AuctionService auctionService) {
    this.bidService = bidService;
    this.userService = userService;
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    int auctionId = 0;
    int bidderId = 0;
    try {
      if (!clientHandler.isAuthenticated()) {
        sendError(clientHandler, "Authentication required");
        return;
      }
      PlaceBidRequest request = packet.getData(PlaceBidRequest.class);
      if (request == null) {
        sendError(clientHandler, "Dữ liệu đặt giá không hợp lệ.");
        return;
      }
      auctionId = request.auctionId();
      long bidAmount = request.bidAmount();
      if (auctionId <= 0) {
        sendError(clientHandler, "Phiên đấu giá không hợp lệ.");
        return;
      }
      if (bidAmount <= 0) {
        sendError(clientHandler, "Giá đặt không hợp lệ.");
        return;
      }
      User user = clientHandler.getUser();
      if (user.getRole() != UserRole.BIDDER) {
        sendError(clientHandler, "Chỉ Bidder được đặt giá.");
        return;
      }
      bidderId = user.getId();
      Auction updatedAuction = bidService.placeBid(auctionId, bidderId, bidAmount);
      PlaceBidResponse response =
          new PlaceBidResponse(updatedAuction.getId(), updatedAuction.getHighestBid(), bidderId);
      auctionService.invalidateCache();
      PacketRes packetResponse =
          PacketRes.of(PacketType.PLACE_BID, "Đặt giá thành công.", response);
      clientHandler.sendPacket(packetResponse);
      Server.broadcastToAuctionViewers(
          auctionId,
          PacketRes.of(PacketType.BID_PLACED, "Có lượt đặt giá mới.", response),
          bidderId);
      sendWalletUpdate(clientHandler, userService.getById(bidderId));
      broadcastAuctionSumList();
      broadcastAuctionDetail(auctionId);
      logger.info("User {} placed bid {} in auction {}", bidderId, bidAmount, auctionId);
    } catch (ServiceException e) {
      logger.warn("Place bid failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected place bid error", e);
      sendError(clientHandler, "Không thể đặt giá.");
    }
  }

  private void sendWalletUpdate(ClientHandler clientHandler, User user) {
    WalletUpdateResponse response = new WalletUpdateResponse(DtoMapper.toUserData(user));
    clientHandler.sendPacket(PacketRes.of(PacketType.WALLET_UPDATED, "OK", response));
  }

  private void broadcastAuctionSumList() {
    try {
      AuctionSummariesResponse response =
          new AuctionSummariesResponse(auctionService.getAuctionSummaries());
      Server.broadcast(PacketRes.of(PacketType.AUCTION_SUMMARIES_UPDATED, "OK", response), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction list", e);
    }
  }

  private void broadcastAuctionDetail(int auctionId) {
    try {
      var auction = auctionService.getAuction(auctionId);
      AuctionDetailResponse response =
          new AuctionDetailResponse(DtoMapper.toAuctionDetail(auction.auction(), auction.item()));
      Server.broadcastToAuctionViewers(
          auctionId, PacketRes.of(PacketType.AUCTION_DETAIL_UPDATED, "OK", response), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction detail", e);
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.PLACE_BID, message));
  }
}
