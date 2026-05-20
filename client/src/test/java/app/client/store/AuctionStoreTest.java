package app.client.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.common.dto.AuctionSummary;
import app.common.enums.AuctionStatus;
import app.common.models.Auction;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuctionStoreTest {

  @Test
  void mergeAuction_ignoresIncomingOlderVersion() {
    AuctionStore store = AuctionStore.getInstance();
    int auctionId = 910001;

    Auction newest =
        new Auction(
            auctionId,
            101,
            201,
            null,
            AuctionStatus.RUNNING,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            5000,
            0,
            5,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now());
    Auction stale =
        new Auction(
            auctionId,
            101,
            201,
            null,
            AuctionStatus.RUNNING,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            3000,
            0,
            3,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now());

    store.addAuction(newest);
    store.addAuction(stale);

    Auction cached = store.getAuction(auctionId);
    assertEquals(5, cached.getVersion());
    assertEquals(5000, cached.getHighestBid());
  }

  @Test
  void setHistorySummaries_deduplicatesAndSkipsInvalidIds() {
    AuctionStore store = AuctionStore.getInstance();
    store.clearHistory();

    int validId = 910101;
    AuctionSummary summary =
        new AuctionSummary(
            validId, "Laptop", 10000, LocalDateTime.now().plusDays(1), AuctionStatus.RUNNING, 1);

    store.setHistorySummaries(
        List.of(
            summary,
            new AuctionSummary(0, "Invalid", 1, LocalDateTime.now(), AuctionStatus.RUNNING, 1),
            summary));

    List<AuctionSummary> history = store.getHistorySummaries();
    assertEquals(1, history.size());
    assertEquals(validId, history.get(0).auctionId());
  }
}
