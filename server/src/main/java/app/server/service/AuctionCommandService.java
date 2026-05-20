package app.server.service;

import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.Bid;
import app.common.models.Item;
import app.common.models.ItemFactory;
import app.common.models.User;
import app.server.dao.AuctionDAO;
import app.server.dao.BidDAO;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.database.TransactionManager;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Write-side auction operations. */
public class AuctionCommandService {
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO;
  private final ItemDAO itemDAO;
  private final UserDAO userDAO;
  private final TransactionManager transactionManager;
  private final AuctionSettlementService settlementService;
  private final Logger logger = LoggerFactory.getLogger(AuctionCommandService.class);
  private final Clock clock;

  public AuctionCommandService(
      AuctionDAO auctionDAO,
      BidDAO bidDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      TransactionManager transactionManager,
      AuctionSettlementService settlementService,
      Clock clock) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
    this.itemDAO = itemDAO;
    this.userDAO = userDAO;
    this.transactionManager = transactionManager;
    this.settlementService = settlementService;
    this.clock = clock;
  }

  public Auction createAndStartAuctionWithItem(
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      ItemType type,
      int durationMinutes,
      int requesterId,
      UserRole requesterRole) {
    validateCreateAuctionRequest(
        name,
        description,
        startingPrice,
        stepPrice,
        type,
        durationMinutes,
        requesterId,
        requesterRole);
    return transactionManager.runInTransaction(
        conn -> {
          Item item =
              ItemFactory.createItem(
                  name, requesterId, description, startingPrice, stepPrice, type);
          Item savedItem = itemDAO.save(conn, item);
          Auction auction =
              new Auction(
                  savedItem.getId(),
                  requesterId,
                  LocalDateTime.now(clock).plusMinutes(durationMinutes),
                  savedItem.getStartingPrice());
          Auction created = auctionDAO.save(conn, auction);
          created.start();
          auctionDAO.update(conn, created);
          return created;
        });
  }

  public void cancelAuction(int auctionId, int requester, int expectedVersion) {
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          ensureCancelPermission(conn, auction, requester);
          AuctionStatus status = auction.getStatus();
          if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID) {
            throw new ServiceException("Không thể hủy phiên đã kết thúc hoặc đã thanh toán.");
          }
          auction.cancel();
          settlementService.releaseWallets(conn, auction);
          boolean ok = auctionDAO.updateIfVersionMatches(conn, auction, expectedVersion);
          if (!ok) {
            throw new ServiceException(
                "Dữ liệu phiên đã thay đổi, vui lòng tải lại trước khi duyệt.");
          }
        });
  }

  public AuctionCompletion completeAuction(int auctionId) {
    return transactionManager.runInTransaction(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          Optional<Bid> highestBid = bidDAO.findHighestBid(conn, auctionId);
          if (!auction.isExpired()) {
            return new AuctionCompletion(auctionId, false, highestBid, Set.of());
          }
          AuctionStatus status = auction.getStatus();
          if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
            return new AuctionCompletion(auctionId, false, highestBid, Set.of());
          }
          auction.finish(highestBid.map(Bid::getBidderId).orElse(null));
          highestBid.ifPresentOrElse(
              bid ->
                  logger.info(
                      "Phiên {} kết thúc. Winner: {}, Giá: {}",
                      auctionId,
                      bid.getBidderName(),
                      bid.getAmount()),
              () -> logger.info("Phiên {} kết thúc. Không có bid."));
          Set<Integer> settledUserIds = settlementService.settleWallets(conn, auction);
          auctionDAO.update(conn, auction);
          return new AuctionCompletion(auctionId, true, highestBid, settledUserIds);
        });
  }

  private Auction requireAuction(java.sql.Connection conn, int auctionId) {
    auctionDAO.lockRow(conn, auctionId);
    return auctionDAO
        .findById(conn, auctionId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
  }

  private void ensureCancelPermission(java.sql.Connection conn, Auction auction, int requesterId) {
    if (requesterId == auction.getSellerId()) {
      return;
    }
    User user =
        userDAO
            .findById(conn, requesterId)
            .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + requesterId));
    if (user.getRole() != UserRole.ADMIN) {
      throw new ServiceException("Bạn không có quyền hủy phiên đấu giá này.");
    }
  }

  private void validateCreateAuctionRequest(
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      ItemType type,
      int durationMinutes,
      int requesterId,
      UserRole requesterRole) {
    if (requesterId <= 0 || requesterRole == null) {
      throw new ServiceException("Dữ liệu người tạo phiên không hợp lệ.");
    }
    if (requesterRole != UserRole.SELLER && requesterRole != UserRole.ADMIN) {
      throw new ServiceException("Chỉ Seller/Admin được tạo phiên.");
    }
    if (name == null
        || name.isBlank()
        || description == null
        || description.isBlank()
        || startingPrice <= 0
        || stepPrice <= 0
        || durationMinutes <= 0
        || type == null) {
      throw new ServiceException("Dữ liệu phiên đấu giá không hợp lệ.");
    }
  }
}
