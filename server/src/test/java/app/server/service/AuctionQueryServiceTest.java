package app.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.TestFixtures;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuctionQueryServiceTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private AuctionQueryService queryService;
  private User seller;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    queryService = new AuctionQueryService();
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void toAuctionSummaries_shouldMapAllSnapshots() {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    List<AuctionSnapshot> snapshots = List.of(new AuctionSnapshot(auction, item));
    var summaries = queryService.toAuctionSummaries(snapshots);

    assertEquals(1, summaries.size());
    assertEquals(auction.getId(), summaries.get(0).auctionId());
  }

  @Test
  void filterHistorySnapshots_shouldApplyPredicate() {
    User bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Watch", ItemType.ART));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    bidDAO.insertBid(auction.getId(), bidder.getId(), 1500L, false);

    List<AuctionSnapshot> snapshots = List.of(new AuctionSnapshot(auction, item));
    var filtered =
        queryService.filterHistorySnapshots(
            snapshots,
            snapshot ->
                snapshot.auction().getSellerId() == bidder.getId()
                    || bidDAO.existsByAuctionAndUser(snapshot.auctionId(), bidder.getId()));

    assertEquals(1, filtered.size());
    assertEquals(auction.getId(), filtered.get(0).auctionId());
  }
}
