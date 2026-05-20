package app.client.manager;

import app.client.store.AuctionStore;
import app.client.utils.AlertUtils;
import app.common.dto.AuctionDetail;
import app.common.dto.AuctionSummary;
import app.common.enums.View;
import app.common.mapper.DtoMapper;
import java.io.IOException;
import javafx.application.Platform;

/** Opens auction details after ensuring the full model is cached. */
public final class AuctionNavigator {
  private static volatile AuctionNavigator instance;

  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();

  private AuctionNavigator() {}

  /** getInstance. */
  public static AuctionNavigator getInstance() {
    if (instance == null) {
      synchronized (AuctionNavigator.class) {
        if (instance == null) {
          instance = new AuctionNavigator();
        }
      }
    }
    return instance;
  }

  /** open. */
  public void open(AuctionSummary summary) {
    if (summary == null) {
      return;
    }
    AuctionStore.getInstance().addAuction(DtoMapper.toAuction(summary));
    LiveAuctionSessionStore.getInstance().selectAuction(summary.auctionId());
    navigateToLive();
    AuctionDetail detail = AuctionStore.getInstance().getAuctionDetail(summary.auctionId());
    if (detail != null) {
      LiveAuctionSessionStore.getInstance().setSelectedDetail(detail);
      notifications.notifyUpdate();
      return;
    }
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Vui lòng kết nối lại!");
      return;
    }
    if (UserManager.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }
    requestAndOpen(summary.auctionId());
  }

  private void requestAndOpen(int auctionId) {
    PendingOpen pending = new PendingOpen(auctionId);
    pending.register();
    try {
      requests.fetchAuctionDetail(auctionId, -1);
    } catch (IOException e) {
      pending.unregister();
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private final class PendingOpen {
    private final int auctionId;
    private final Runnable updateListener;
    private final java.util.function.Consumer<String> messageListener;

    private PendingOpen(int auctionId) {
      this.auctionId = auctionId;
      this.updateListener = () -> Platform.runLater(this::onUpdate);
      this.messageListener = message -> Platform.runLater(() -> onMessage(message));
    }

    private void register() {
      notifications.addUpdateListener(updateListener);
      notifications.addMessageListener(messageListener);
    }

    private void unregister() {
      notifications.removeUpdateListener(updateListener);
      notifications.removeMessageListener(messageListener);
    }

    private void onUpdate() {
      AuctionDetail detail = AuctionStore.getInstance().getAuctionDetail(auctionId);
      if (detail == null) {
        return;
      }
      unregister();
      LiveAuctionSessionStore.getInstance().setSelectedDetail(detail);
      notifications.notifyUpdate();
    }

    private void onMessage(String message) {
      if (message == null || message.isBlank()) {
        return;
      }
      AuctionDetail detail = AuctionStore.getInstance().getAuctionDetail(auctionId);
      if (detail != null) {
        return;
      }
      unregister();
      AlertUtils.showError("Lỗi", message);
    }
  }

  private void navigateToLive() {
    NavigationManager.getInstance().navigateTo(View.LIVE);
  }
}
