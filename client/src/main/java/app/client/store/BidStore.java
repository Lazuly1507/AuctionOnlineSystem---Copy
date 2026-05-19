package app.client.store;

import app.common.models.Bid;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BidStore {
  private static volatile BidStore instance;

  private final Map<Integer, Bid> bidMap = new ConcurrentHashMap<>();

  private BidStore() {}

  /** getInstance. */
  public static BidStore getInstance() {
    if (instance == null) {
      synchronized (BidStore.class) {
        if (instance == null) {
          instance = new BidStore();
        }
      }
    }
    return instance;
  }

  public void addBid(Bid bid) {
    if (bid == null) {
      return;
    }
    this.bidMap.put(bid.getId(), bid);
  }

  public Bid getBid(int bidId) {
    return this.bidMap.get(bidId);
  }
}
