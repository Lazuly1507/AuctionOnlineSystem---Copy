package app.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.TestFixtures;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Auction;
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
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuctionCommandServiceTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private TransactionManager transactionManager;
  private AuctionCommandService commandService;
  private User seller;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    transactionManager = new TransactionManager();
    commandService =
        new AuctionCommandService(
            auctionDAO,
            bidDAO,
            itemDAO,
            userDAO,
            transactionManager,
            new AuctionSettlementService(bidDAO, userDAO),
            Clock.systemDefaultZone());
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void createAndStartAuctionWithItem_shouldCreateRunningAuction() {
    Auction created =
        commandService.createAndStartAuctionWithItem(
            "Mic", "Test", 1000, 100, ItemType.ELECTRONICS, 5, seller.getId(), seller.getRole());

    Auction saved = auctionDAO.findById(created.getId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, saved.getStatus());
  }

  @Test
  void cancelAuction_shouldRejectUnauthorizedUser() {
    User other = userDAO.save(TestFixtures.user(TestFixtures.unique("other"), UserRole.SELLER));
    var item = itemDAO.save(TestFixtures.item(seller.getId(), "Cam", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    assertThrows(
        ServiceException.class,
        () -> commandService.cancelAuction(auction.getId(), other.getId(), auction.getVersion()));
  }
}
