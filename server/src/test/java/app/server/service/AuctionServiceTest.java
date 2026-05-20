package app.server.service;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.Item;
import app.common.models.User;
import app.server.dao.AuctionDAO;
import app.server.dao.BaseDAOTest;
import app.server.dao.BidDAO;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.dao.impl.MySqlAuctionDAO;
import app.server.dao.impl.MySqlBidDAO;
import app.server.dao.impl.MySqlItemDAO;
import app.server.dao.impl.MySqlUserDAO;
import app.server.database.TransactionManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuctionServiceTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private AuctionService auctionService;
  private TransactionManager transactionManager;
  private User seller;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    transactionManager = new TransactionManager();
    auctionService = new AuctionService(auctionDAO, bidDAO, itemDAO, userDAO, transactionManager);
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void createAndStartAuctionWithItem_shouldPersistItemAndRunningAuction() {
    Auction created =
        auctionService.createAndStartAuctionWithItem(
            "Camera",
            "Test camera",
            1000L,
            100L,
            ItemType.ELECTRONICS,
            10,
            seller.getId(),
            seller.getRole());

    Auction found = auctionDAO.findById(created.getId()).orElseThrow();
    Item item = itemDAO.findById(found.getItemId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, found.getStatus());
    assertEquals("Camera", item.getName());
    assertEquals(seller.getId(), item.getSellerId());
  }

  @Test
  void completeAndGetHighestBid_shouldReturnHighestBidWhenBidsExist() {
    User bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    bidDAO.insertBid(auction.getId(), bidder.getId(), 1500L, false);

    var highestBid = auctionService.completeAndGetHighestBid(auction.getId()).orElseThrow();

    assertEquals(auction.getId(), highestBid.getAuctionId());
    assertEquals(bidder.getId(), highestBid.getBidderId());
    assertEquals(1500L, highestBid.getAmount());
  }

  @Test
  void completeAndGetHighestBid_shouldFinishExpiredAuctionAndSetWinner() {
    User bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Tablet", ItemType.ELECTRONICS));
    Auction auction =
        TestFixtures.auction(
            item.getId(), seller.getId(), LocalDateTime.now().minusMinutes(1), 1000L);
    auction.start();
    auction = auctionDAO.save(auction);
    bidDAO.insertBid(auction.getId(), bidder.getId(), 1500L, false);

    auctionService.completeAndGetHighestBid(auction.getId());

    Auction finished = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.FINISHED, finished.getStatus());
    assertEquals(bidder.getId(), finished.getWinnerId());
  }

  @Test
  void cancelAuction_shouldAllowSellerOwner() {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Speaker", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    auctionService.cancelAuction(auction.getId(), seller.getId(), auction.getVersion());

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.CANCELED, found.getStatus());
  }

  @Test
  void cancelAuction_shouldAllowAdmin() {
    User admin = userDAO.save(TestFixtures.user(TestFixtures.unique("admin"), UserRole.ADMIN));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Speaker", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    auctionService.cancelAuction(auction.getId(), admin.getId(), auction.getVersion());

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.CANCELED, found.getStatus());
  }

  @Test
  void cancelAuction_shouldRejectNonOwnerNonAdmin() {
    User otherSeller =
        userDAO.save(TestFixtures.user(TestFixtures.unique("other_seller"), UserRole.SELLER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Speaker", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    assertThrows(
        ServiceException.class,
        () ->
            auctionService.cancelAuction(
                auction.getId(), otherSeller.getId(), auction.getVersion()));

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.OPEN, found.getStatus());
  }

  @Test
  void cancelAuction_shouldRejectStaleVersion() {
    User admin = userDAO.save(TestFixtures.user(TestFixtures.unique("admin"), UserRole.ADMIN));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Speaker", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    int staleVersion = auction.getVersion();
    auction.setStatus(AuctionStatus.RUNNING);
    auctionDAO.update(auction);

    assertThrows(
        ServiceException.class,
        () -> auctionService.cancelAuction(auction.getId(), admin.getId(), staleVersion));

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, found.getStatus());
  }

  @Test
  void getAuctions_shouldUseCacheUntilInvalidated() {
    Item firstItem = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    auctionDAO.save(
        TestFixtures.auction(
            firstItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    var firstLoad = auctionService.getAuctions();

    Item secondItem = itemDAO.save(TestFixtures.item(seller.getId(), "Bike", ItemType.VEHICLE));
    auctionDAO.save(
        TestFixtures.auction(
            secondItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 2000L));
    var cachedLoad = auctionService.getAuctions();

    assertEquals(1, firstLoad.size());
    assertEquals(1, cachedLoad.size());

    auctionService.invalidateCache();
    var refreshed = auctionService.getAuctions();

    assertEquals(2, refreshed.size());
  }

  @Test
  void getAuctionSummaries_shouldMapFromCachedSnapshots() {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Camera", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    var summaries = auctionService.getAuctionSummaries();

    assertEquals(1, summaries.size());
    assertEquals(auction.getId(), summaries.get(0).auctionId());
  }

  @Test
  void getHistorySummaries_shouldFilterBySellerOrBidder() {
    User bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    bidDAO.insertBid(auction.getId(), bidder.getId(), 1500L, false);

    var sellerHistory = auctionService.getHistorySummaries(seller.getId());
    var bidderHistory = auctionService.getHistorySummaries(bidder.getId());

    assertEquals(1, sellerHistory.size());
    assertEquals(1, bidderHistory.size());
    assertEquals(auction.getId(), sellerHistory.get(0).auctionId());
    assertEquals(auction.getId(), bidderHistory.get(0).auctionId());
  }
}
