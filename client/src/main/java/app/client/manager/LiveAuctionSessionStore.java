package app.client.manager;

import app.common.dto.AuctionDetail;

/** Session-scoped state for the currently opened live auction screen. */
public final class LiveAuctionSessionStore {
  private static volatile LiveAuctionSessionStore instance;

  private Integer selectedAuctionId;
  private AuctionDetail selectedDetail;

  private LiveAuctionSessionStore() {}

  /** getInstance. */
  public static LiveAuctionSessionStore getInstance() {
    if (instance == null) {
      synchronized (LiveAuctionSessionStore.class) {
        if (instance == null) {
          instance = new LiveAuctionSessionStore();
        }
      }
    }
    return instance;
  }

  public synchronized void selectAuction(int auctionId) {
    selectedAuctionId = auctionId;
    selectedDetail = null;
  }

  public synchronized Integer getSelectedAuctionId() {
    return selectedAuctionId;
  }

  public synchronized AuctionDetail getSelectedDetail() {
    return selectedDetail;
  }

  public synchronized void setSelectedDetail(AuctionDetail detail) {
    selectedDetail = detail;
    if (detail != null && detail.auction() != null) {
      selectedAuctionId = detail.auction().id();
    }
  }

  public synchronized void clear() {
    selectedAuctionId = null;
    selectedDetail = null;
  }
}
