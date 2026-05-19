package app.server.service;

import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.*;
import app.server.dao.AuctionDAO;
import app.server.dao.BidDAO;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.database.TransactionManager;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionService {
  private volatile List<AuctionSnapshot> snapshotCache;
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO;
  private final ItemDAO itemDAO;
  private final UserDAO userDAO;
  private final TransactionManager transactionManager;
  private final Logger logger = LoggerFactory.getLogger(AuctionService.class);
  private final Clock clock;

  public AuctionService(
      AuctionDAO auctionDAO,
      BidDAO bidDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      TransactionManager transactionManager) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
    this.itemDAO = itemDAO;
    this.userDAO = userDAO;
    this.transactionManager = transactionManager;
    this.clock = Clock.systemDefaultZone();
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
    Auction createdAuction =
        transactionManager.runInTransaction(
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
    invalidateCache();
    return createdAuction;
  }

  public List<AuctionSnapshot> getAuctions() {
    List<AuctionSnapshot> cached = snapshotCache;
    if (cached != null) {
      return cached;
    }
    snapshotCache = auctionDAO.findAll().stream().map(this::toSnapshot).toList();
    return snapshotCache;
  }

  public List<AuctionSnapshot> getHistoryAuctions(int userId) {
    return getAuctions().stream()
        .filter(snapshot -> isSellerOrBidder(snapshot.auction(), userId))
        .toList();
  }

  public AuctionSnapshot getAuction(int auctionId) {
    List<AuctionSnapshot> cached = snapshotCache;
    if (cached != null) {
      for (AuctionSnapshot snapshot : cached) {
        if (snapshot.auctionId() == auctionId) {
          return snapshot;
        }
      }
    }
    Auction auction =
        auctionDAO
            .findById(auctionId)
            .orElseThrow(() -> new ServiceException("Không tìm thấy phiên ID: " + auctionId));
    return toSnapshot(auction);
  }

  public boolean isAuctionVersionCurrent(int auctionId, int knownVersion) {
    if (knownVersion < 0) {
      return false;
    }
    return getAuction(auctionId).version() == knownVersion;
  }

  public Optional<Bid> completeAndGetHighestBid(int auctionId) {
    return completeAuction(auctionId).highestBid();
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
          releaseWallets(conn, auction);
          boolean ok = auctionDAO.updateIfVersionMatches(conn, auction, expectedVersion);
          if (!ok) {
            throw new ServiceException(
                "Dữ liệu phiên đã thay đổi, vui lòng tải lại trước khi duyệt.");
          }
        });
    invalidateCache();
  }

  public AuctionCompletion completeAuction(int auctionId) {
    AuctionCompletion completion =
        transactionManager.runInTransaction(
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
                  () -> logger.info("Phiên {} kết thúc. Không có bid.", auctionId));
              Set<Integer> settledUserIds = settleWallets(conn, auction);
              auctionDAO.update(conn, auction);
              return new AuctionCompletion(auctionId, true, highestBid, settledUserIds);
            });
    if (completion.completed()) {
      invalidateCache();
    }
    return completion;
  }

  public List<Integer> completeExpiredAuctions() {
    return completeExpiredAuctionCompletions().stream().map(AuctionCompletion::auctionId).toList();
  }

  public List<AuctionCompletion> completeExpiredAuctionCompletions() {
    List<AuctionCompletion> completions = new ArrayList<>();
    for (Auction auction : auctionDAO.findAll()) {
      if (!auction.isExpired()) {
        continue;
      }
      AuctionStatus status = auction.getStatus();
      if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
        continue;
      }
      AuctionCompletion completion = completeAuction(auction.getId());
      if (completion.completed()) {
        completions.add(completion);
      }
    }
    return completions;
  }

  private Auction requireAuction(java.sql.Connection conn, int auctionId) {
    auctionDAO.lockRow(conn, auctionId);
    return auctionDAO
        .findById(conn, auctionId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
  }

  private Set<Integer> settleWallets(java.sql.Connection conn, Auction auction) {
    List<Bid> bids = bidDAO.findByAuction(conn, auction.getId());
    Set<Integer> bidderIds = new LinkedHashSet<>();
    for (Bid bid : bids) {
      bidderIds.add(bid.getBidderId());
    }
    for (Integer bidderId : bidderIds) {
      userDAO.lockRow(conn, bidderId);
      User user =
          userDAO
              .findById(conn, bidderId)
              .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + bidderId));
      if (auction.getWinnerId() != null && auction.getWinnerId() == bidderId) {
        user.getWallet().commitFrozen(String.valueOf(auction.getId()));
      } else {
        user.getWallet().releaseFrozen(String.valueOf(auction.getId()));
      }
      userDAO.update(conn, user);
    }
    return bidderIds;
  }

  private void releaseWallets(java.sql.Connection conn, Auction auction) {
    List<Bid> bids = bidDAO.findByAuction(conn, auction.getId());
    Set<Integer> bidderIds = new LinkedHashSet<>();
    for (Bid bid : bids) {
      bidderIds.add(bid.getBidderId());
    }
    for (Integer bidderId : bidderIds) {
      userDAO.lockRow(conn, bidderId);
      User user =
          userDAO
              .findById(conn, bidderId)
              .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + bidderId));
      user.getWallet().releaseFrozen(String.valueOf(auction.getId()));
      userDAO.update(conn, user);
    }
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

  public void invalidateCache() {
    snapshotCache = null;
    logger.info("[CACHE] Auction cache invalidated");
  }

  private boolean isSellerOrBidder(Auction auction, int userId) {
    return auction.getSellerId() == userId
        || bidDAO.existsByAuctionAndUser(auction.getId(), userId);
  }

  private AuctionSnapshot toSnapshot(Auction auction) {
    Item item =
        itemDAO
            .findById(auction.getItemId())
            .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
    return new AuctionSnapshot(auction, item);
  }
}
