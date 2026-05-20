package app.client.manager;

import app.client.controllers.Cleanable;
import app.client.store.AuctionStore;
import app.client.utils.AlertUtils;
import app.common.dto.AuctionData;
import app.common.dto.AuctionDetail;
import app.common.dto.AuctionSummary;
import app.common.dto.ItemData;
import app.common.enums.View;
import app.common.mapper.DtoMapper;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** NavigationManager. */
public class NavigationManager {
  private static final NavigationManager instance = new NavigationManager();
  private final Logger logger = LoggerFactory.getLogger(NavigationManager.class);
  private Stage primaryStage;
  private Object currentController;
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();

  private NavigationManager() {}

  public static NavigationManager getInstance() {
    return instance;
  }

  public void setPrimaryStage(Stage stage) {
    this.primaryStage = stage;
  }

  /** navigateTo. */
  public void navigateTo(View view) {
    navigateTo(view, null);
  }

  /** navigateTo. */
  public void navigateTo(View view, Consumer<Object> controllerCallback) {
    try {
      if (currentController instanceof Cleanable cleanable) {
        cleanable.cleanup();
      }
      FXMLLoader loader = new FXMLLoader(getClass().getResource(view.getFxmlPath()));
      Parent root = loader.load();
      Object newController = loader.getController();
      if (controllerCallback != null) {
        controllerCallback.accept(newController);
      }
      currentController = newController;
      Scene scene = new Scene(root);
      String css =
          Objects.requireNonNull(getClass().getResource("/app/views/style.css")).toExternalForm();
      scene.getStylesheets().add(css);
      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (IOException e) {
      logger.warn("Lỗi nghiêm trọng: Không thể load màn hình " + view.name());
      e.printStackTrace();
    }
  }

  /** openAuctionDetail. */
  public void openAuctionDetail(AuctionSummary summary) {
    if (summary == null) {
      return;
    }
    AuctionStore.getInstance().addAuction(DtoMapper.toAuction(summary));
    LiveAuctionSessionStore.getInstance().selectAuction(summary.auctionId());
    LiveAuctionSessionStore.getInstance().setSelectedDetail(createPlaceholderDetail(summary));
    navigateTo(View.LIVE);
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
      navigateTo(View.LOGIN);
      return;
    }
    requestAndOpen(summary.auctionId());
  }

  private AuctionDetail createPlaceholderDetail(AuctionSummary summary) {
    AuctionData auction =
        new AuctionData(
            summary.auctionId(),
            0,
            0,
            null,
            summary.status(),
            null,
            summary.endTime(),
            summary.currentPrice(),
            0,
            summary.version(),
            null,
            null);
    ItemData item =
        new ItemData(0, 0, summary.itemName(), "Đang tải chi tiết vật phẩm...", 0, 0, null, false);
    return new AuctionDetail(auction, item);
  }

  private void requestAndOpen(int auctionId) {
    PendingDetailRequest pending = new PendingDetailRequest(auctionId);
    pending.register();
    try {
      requests.fetchAuctionDetail(auctionId, -1);
    } catch (IOException e) {
      pending.unregister();
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private final class PendingDetailRequest implements Runnable, Consumer<String> {
    private final int auctionId;

    private PendingDetailRequest(int auctionId) {
      this.auctionId = auctionId;
    }

    private void register() {
      notifications.addUpdateListener(this);
      notifications.addMessageListener(this);
    }

    private void unregister() {
      notifications.removeUpdateListener(this);
      notifications.removeMessageListener(this);
    }

    @Override
    public void run() {
      Platform.runLater(
          () -> {
            AuctionDetail detail = AuctionStore.getInstance().getAuctionDetail(auctionId);
            if (detail == null) {
              return;
            }
            unregister();
            LiveAuctionSessionStore.getInstance().setSelectedDetail(detail);
            notifications.notifyUpdate();
          });
    }

    @Override
    public void accept(String message) {
      Platform.runLater(
          () -> {
            if (message == null || message.isBlank()) {
              return;
            }
            AuctionDetail detail = AuctionStore.getInstance().getAuctionDetail(auctionId);
            if (detail != null) {
              return;
            }
            unregister();
            AlertUtils.showError("Lỗi", message);
          });
    }
  }
}
