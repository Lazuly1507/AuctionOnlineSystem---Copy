package app.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import app.server.database.TransactionManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuctionSettlementServiceTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private TransactionManager transactionManager;
  private AuctionSettlementService settlementService;
  private User seller;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    transactionManager = new TransactionManager();
    settlementService = new AuctionSettlementService(bidDAO, userDAO);
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void settleWallets_shouldCommitWinnerAndReleaseOthers() {
    User winner = userDAO.save(TestFixtures.user(TestFixtures.unique("winner"), UserRole.BIDDER));
    User loser = userDAO.save(TestFixtures.user(TestFixtures.unique("loser"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    winner.getWallet().setFrozenAmount(String.valueOf(auction.getId()), new BigDecimal("500"));
    loser.getWallet().setFrozenAmount(String.valueOf(auction.getId()), new BigDecimal("300"));
    userDAO.update(winner);
    userDAO.update(loser);

    bidDAO.insertBid(auction.getId(), winner.getId(), 1500L, false);
    bidDAO.insertBid(auction.getId(), loser.getId(), 1400L, false);
    auction.setWinnerId(winner.getId());

    transactionManager.runWithoutResult(conn -> settlementService.settleWallets(conn, auction));

    User winnerAfter = userDAO.findById(winner.getId()).orElseThrow();
    User loserAfter = userDAO.findById(loser.getId()).orElseThrow();
    assertEquals(
        BigDecimal.ZERO, winnerAfter.getWallet().getFrozenAmount(String.valueOf(auction.getId())));
    assertEquals(
        BigDecimal.ZERO, loserAfter.getWallet().getFrozenAmount(String.valueOf(auction.getId())));
    assertTrue(
        winner
                .getWallet()
                .getAvailableBalance()
                .compareTo(winnerAfter.getWallet().getAvailableBalance())
            == 0);
    assertTrue(
        loser
                .getWallet()
                .getAvailableBalance()
                .add(new BigDecimal("300"))
                .compareTo(loserAfter.getWallet().getAvailableBalance())
            == 0);
  }

  @Test
  void releaseWallets_shouldReleaseAllFrozenFunds() {
    User bidderOne =
        userDAO.save(TestFixtures.user(TestFixtures.unique("bidder_one"), UserRole.BIDDER));
    User bidderTwo =
        userDAO.save(TestFixtures.user(TestFixtures.unique("bidder_two"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Tablet", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    bidderOne.getWallet().setFrozenAmount(String.valueOf(auction.getId()), new BigDecimal("200"));
    bidderTwo.getWallet().setFrozenAmount(String.valueOf(auction.getId()), new BigDecimal("350"));
    userDAO.update(bidderOne);
    userDAO.update(bidderTwo);

    bidDAO.insertBid(auction.getId(), bidderOne.getId(), 1200L, false);
    bidDAO.insertBid(auction.getId(), bidderTwo.getId(), 1300L, false);

    transactionManager.runWithoutResult(conn -> settlementService.releaseWallets(conn, auction));

    User bidderOneAfter = userDAO.findById(bidderOne.getId()).orElseThrow();
    User bidderTwoAfter = userDAO.findById(bidderTwo.getId()).orElseThrow();
    assertEquals(
        BigDecimal.ZERO,
        bidderOneAfter.getWallet().getFrozenAmount(String.valueOf(auction.getId())));
    assertEquals(
        BigDecimal.ZERO,
        bidderTwoAfter.getWallet().getFrozenAmount(String.valueOf(auction.getId())));
    assertTrue(
        bidderOne
                .getWallet()
                .getAvailableBalance()
                .add(new BigDecimal("200"))
                .compareTo(bidderOneAfter.getWallet().getAvailableBalance())
            == 0);
    assertTrue(
        bidderTwo
                .getWallet()
                .getAvailableBalance()
                .add(new BigDecimal("350"))
                .compareTo(bidderTwoAfter.getWallet().getAvailableBalance())
            == 0);
  }
}
