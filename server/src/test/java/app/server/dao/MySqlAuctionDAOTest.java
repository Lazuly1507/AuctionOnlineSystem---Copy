package app.server.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.TestFixtures;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.models.Auction;
import app.common.models.Item;
import app.common.models.User;
import app.server.dao.impl.MySqlAuctionDAO;
import app.server.dao.impl.MySqlItemDAO;
import app.server.dao.impl.MySqlUserDAO;
import app.server.database.DatabaseConnection;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MySqlAuctionDAOTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private User seller;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void save_shouldPersistAuctionAndReturnGeneratedId() {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Auction saved =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    assertTrue(saved.getId() > 0);
    Auction found = auctionDAO.findById(saved.getId()).orElseThrow();
    assertEquals(item.getId(), found.getItemId());
    assertEquals(seller.getId(), found.getSellerId());
    assertEquals(AuctionStatus.OPEN, found.getStatus());
    assertEquals(1000L, found.getHighestBid());
    assertEquals(0, found.getVersion());
  }

  @Test
  void update_shouldPersistStatusWinnerAndBidState() {
    User bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Camera", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    auction.start();
    auction.updateHighestBid(1500L, bidder.getId());
    auction.setStatus(AuctionStatus.FINISHED);
    auction.setEndTime(LocalDateTime.now().plusMinutes(10));

    boolean updated = auctionDAO.update(auction);

    assertTrue(updated);
    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.FINISHED, found.getStatus());
    assertEquals(bidder.getId(), found.getWinnerId());
    assertEquals(1500L, found.getHighestBid());
    assertTrue(found.getStartTime() != null);
    assertEquals(auction.getVersion(), found.getVersion());
    assertEquals(1, found.getVersion());
  }

  @Test
  void updateIfVersionMatches_shouldUpdateOnlyWhenExpectedVersionMatches() throws Exception {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Watch", ItemType.ART));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    int initialVersion = auction.getVersion();

    auction.setStatus(AuctionStatus.CANCELED);
    boolean updated;
    try (var conn = DatabaseConnection.getDataSource().getConnection()) {
      updated = auctionDAO.updateIfVersionMatches(conn, auction, initialVersion);
    }

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertTrue(updated);
    assertEquals(AuctionStatus.CANCELED, found.getStatus());
    assertEquals(initialVersion + 1, found.getVersion());

    found.setStatus(AuctionStatus.RUNNING);
    boolean staleUpdate;
    try (var conn = DatabaseConnection.getDataSource().getConnection()) {
      staleUpdate = auctionDAO.updateIfVersionMatches(conn, found, initialVersion);
    }

    Auction unchanged = auctionDAO.findById(auction.getId()).orElseThrow();
    assertFalse(staleUpdate);
    assertEquals(AuctionStatus.CANCELED, unchanged.getStatus());
    assertEquals(initialVersion + 1, unchanged.getVersion());
  }

  @Test
  void findByStatusAndSeller_shouldFilterAuctions() {
    Item firstItem = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Item secondItem = itemDAO.save(TestFixtures.item(seller.getId(), "Bike", ItemType.VEHICLE));
    Auction openAuction =
        auctionDAO.save(
            TestFixtures.auction(
                firstItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    Auction runningAuction =
        auctionDAO.save(
            TestFixtures.auction(
                secondItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 2000L));
    runningAuction.start();
    auctionDAO.update(runningAuction);

    var openAuctions = auctionDAO.findByStatus(AuctionStatus.OPEN);
    var sellerAuctions = auctionDAO.findBySeller(seller.getId());

    assertEquals(1, openAuctions.size());
    assertEquals(openAuction.getId(), openAuctions.getFirst().getId());
    assertEquals(2, sellerAuctions.size());
  }

  @Test
  void delete_shouldRemoveAuction() {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Tablet", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    assertTrue(auctionDAO.delete(auction.getId()));

    assertFalse(auctionDAO.findById(auction.getId()).isPresent());
  }
}
