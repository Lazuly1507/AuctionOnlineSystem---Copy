package app.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class BidServiceTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private BidService bidService;
  private User seller;
  private User bidder;
  private Item item;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    bidService =
        new BidService(
            bidDAO,
            auctionDAO,
            userDAO,
            new TransactionManager(),
            new BidValidator(),
            new AntiSnipeService());
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
    bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    item = itemDAO.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));
  }

  @Test
  void placeBid_shouldInsertBidAndUpdateAuction() {
    Auction auction = runningAuction(1000L, LocalDateTime.now().plusMinutes(10));

    bidService.placeBid(auction.getId(), bidder.getId(), 1200L);

    Auction updated = auctionDAO.findById(auction.getId()).orElseThrow();
    var highest = bidDAO.findHighestBid(auction.getId()).orElseThrow();
    assertEquals(1200L, updated.getHighestBid());
    assertEquals(bidder.getId(), updated.getWinnerId());
    assertEquals(1200L, highest.getAmount());
    assertEquals(bidder.getId(), highest.getBidderId());
  }

  @Test
  void placeBid_shouldAllowHighestBidderToBidAgain() {
    Auction auction = runningAuction(1000L, LocalDateTime.now().plusMinutes(10));

    bidService.placeBid(auction.getId(), bidder.getId(), 1200L);
    bidService.placeBid(auction.getId(), bidder.getId(), 1500L);

    Auction updated = auctionDAO.findById(auction.getId()).orElseThrow();
    var highest = bidDAO.findHighestBid(auction.getId()).orElseThrow();
    assertEquals(1500L, updated.getHighestBid());
    assertEquals(bidder.getId(), updated.getWinnerId());
    assertEquals(1500L, highest.getAmount());
    assertEquals(2, bidDAO.findByAuction(auction.getId()).size());
  }

  @Test
  void placeBid_shouldRejectBidBelowMinimumAndRollback() {
    Auction auction = runningAuction(1000L, LocalDateTime.now().plusMinutes(10));

    assertThrows(
        ServiceException.class, () -> bidService.placeBid(auction.getId(), bidder.getId(), 1000L));

    Auction unchanged = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(1000L, unchanged.getHighestBid());
    assertTrue(bidDAO.findHighestBid(auction.getId()).isEmpty());
  }

  @Test
  void placeBid_shouldRejectAuctionThatIsNotRunning() {
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusMinutes(10), 1000L));

    assertThrows(
        ServiceException.class, () -> bidService.placeBid(auction.getId(), bidder.getId(), 1200L));
  }

  @Test
  void placeBid_shouldExtendAuctionWhenBidIsNearEndTime() {
    LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(10);
    Auction auction = runningAuction(1000L, originalEndTime);

    bidService.placeBid(auction.getId(), bidder.getId(), 1200L);

    Auction updated = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(1, updated.getExtendedCount());
    assertTrue(updated.getEndTime().isAfter(originalEndTime.plusSeconds(50)));
  }

  @Test
  void placeBid_shouldReturnCurrentHighestBid() {
    Auction auction = runningAuction(1000L, LocalDateTime.now().plusMinutes(10));

    var updated = bidService.placeBid(auction.getId(), bidder.getId(), 1300L);

    assertEquals(auction.getId(), updated.getId());
    assertEquals(1300L, updated.getHighestBid());
    assertEquals(bidder.getId(), updated.getWinnerId());
  }

  @Test
  void bidDAOQueries_shouldReturnPersistedBids() {
    Auction auction = runningAuction(1000L, LocalDateTime.now().plusMinutes(10));
    User secondBidder =
        userDAO.save(TestFixtures.user(TestFixtures.unique("second_bidder"), UserRole.BIDDER));
    bidService.placeBid(auction.getId(), bidder.getId(), 1200L);
    bidService.placeBid(auction.getId(), secondBidder.getId(), 1400L);

    assertEquals(2, bidDAO.findByAuction(auction.getId()).size());
    assertEquals(2, bidDAO.findByAuctionOrderByTime(auction.getId()).size());
    assertEquals(1400L, bidDAO.findHighestBid(auction.getId()).orElseThrow().getAmount());
  }

  private Auction runningAuction(long currentPrice, LocalDateTime endTime) {
    Auction auction = TestFixtures.auction(item.getId(), seller.getId(), endTime, currentPrice);
    auction.start();
    auction = auctionDAO.save(auction);
    auctionDAO.update(auction);
    assertEquals(
        AuctionStatus.RUNNING, auctionDAO.findById(auction.getId()).orElseThrow().getStatus());
    return auction;
  }
}
