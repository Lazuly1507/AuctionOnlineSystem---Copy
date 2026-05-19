package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.AuctionData;
import app.common.dto.AuctionDetail;
import app.common.enums.AuctionStatus;
import app.common.enums.View;
import app.common.models.Auction;
import app.common.models.User;
import app.common.models.Wallet;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** LiveController. */
public class LiveController implements Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(LiveController.class);
  private AuctionData auction;
  private long currentPrice;
  private AuctionDetail auctionDetail;
  @FXML private Label itemNameLabel;
  @FXML private Label startPriceLabel;
  @FXML private Label stepPriceLabel;
  @FXML private Label currentPriceLabel;
  @FXML private Label depositLabel;
  @FXML private Label timeLabel;
  @FXML private TextField bidAmountField;
  @FXML private TextArea description;
  @FXML private Label availableBalanceLabel;
  private ScheduledExecutorService scheduler;
  private boolean resultRequested = false;
  private boolean auctionClosedShown = false;
  private boolean cleanedUp = false;
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private boolean bidLoading;
  private Button bidButton;
  private Runnable stopBidLoading = () -> {};
  private final Runnable updateListener = () -> Platform.runLater(this::handleUpdateNotification);
  private final Consumer<String> messageListener =
      message -> Platform.runLater(() -> handleMessageNotification(message));

  /** Member. */
  @FXML
  public void initialize() {
    notifications.addUpdateListener(updateListener);
    notifications.addMessageListener(messageListener);
    updateAvailableBalance();
  }

  /** setAuction. */
  public void setAuction(AuctionDetail detail) {
    if (detail == null || detail.auction() == null) {
      return;
    }
    auctionDetail = detail;
    auction = detail.auction();
    currentPrice = auction.highestBid();
    applyDetail(detail);
  }

  private void updateAvailableBalance() {
    updateAvailableBalance(UserManager.getInstance().getCurrentUser());
  }

  private void updateAvailableBalance(User user) {
    if (availableBalanceLabel == null) {
      return;
    }
    if (user == null) {
      availableBalanceLabel.setText("Số dư: 0 đ");
      return;
    }
    Wallet wallet = user.getWallet();
    BigDecimal available = wallet.getAvailableBalance();
    availableBalanceLabel.setText("Số dư khả dụng: " + formatCurrency(available) + " đ");
  }

  private String formatCurrency(BigDecimal amount) {
    if (amount == null) {
      return "0";
    }
    return currencyFormat.format(amount);
  }

  private String formatCurrency(long amount) {
    return String.format("%,d đ", amount);
  }

  private void handleUpdateNotification() {
    refreshDetailFromStore();
    updateAvailableBalance();
  }

  private void handleMessageNotification(String message) {
    if (message == null || message.isBlank()) {
      return;
    }
    if (bidLoading) {
      handleBidResult(message);
      return;
    }
    if (resultRequested) {
      handleAuctionResultMessage(message);
    }
  }

  private void handleBidResult(String message) {
    setBidLoading(false);
    refreshDetailFromStore();
    bidAmountField.clear();
    if (isFailureMessage(message)) {
      AlertUtils.showError("Đặt giá", message);
      return;
    }
    AlertUtils.showInfo("Đặt giá", message);
  }

  private void handleAuctionResultMessage(String message) {
    resultRequested = false;
    refreshDetailFromStore();
    if (auction == null) {
      return;
    }
    Auction cachedAuction = AuctionStore.getInstance().getAuction(auction.id());
    if (cachedAuction != null && cachedAuction.getStatus() == AuctionStatus.FINISHED) {
      showAuctionClosed(message);
      return;
    }
    AlertUtils.showError("Kết thúc", message);
  }

  private boolean isFailureMessage(String message) {
    String normalized = message == null ? "" : message.toLowerCase();
    return normalized.contains("lỗi") || normalized.contains("thất bại");
  }

  private void refreshDetailFromStore() {
    if (auction == null) {
      return;
    }
    AuctionDetail cachedDetail = AuctionStore.getInstance().getAuctionDetail(auction.id());
    if (cachedDetail != null) {
      auctionDetail = cachedDetail;
      auction = cachedDetail.auction();
      applyDetail(cachedDetail);
      return;
    }
    Auction cachedAuction = AuctionStore.getInstance().getAuction(auction.id());
    if (cachedAuction != null) {
      currentPrice = Math.max(currentPrice, cachedAuction.getHighestBid());
      currentPriceLabel.setText(formatCurrency(currentPrice));
    }
  }

  private void applyDetail(AuctionDetail detail) {
    itemNameLabel.setText(detail.itemName());
    startPriceLabel.setText(formatCurrency(detail.startingPrice()));
    stepPriceLabel.setText(formatCurrency(detail.stepPrice()));
    currentPrice = Math.max(currentPrice, detail.auction().highestBid());
    currentPriceLabel.setText(formatCurrency(currentPrice));
    depositLabel.setText(formatCurrency((long) (detail.startingPrice() * 0.2)));
    description.setText(detail.description());
    startCountdownTimer(detail.endTime());
  }

  /** onNewBidPlaced. */
  public void onNewBidPlaced(String itemName, long newPrice, String bidderName) {
    Platform.runLater(
        () -> {
          currentPrice = Math.max(currentPrice, newPrice);
          currentPriceLabel.setText(formatCurrency(currentPrice));
        });
  }

  /** onAuctionClosed. */
  public void onAuctionClosed(String itemName, String winnerName, long finalPrice) {
    Platform.runLater(
        () -> {
          if (auctionClosedShown) {
            return;
          }
          auctionClosedShown = true;
          AlertUtils.showInfo(
              "Kết thúc",
              "Phiên đấu giá đã kết thúc. Người thắng: "
                  + winnerName
                  + " với giá: "
                  + formatCurrency(finalPrice));
          timeLabel.setText("Phiên đấu giá đã kết thúc!");
          timeLabel.setStyle(
              "-fx-text-fill: red;" + "-fx-font-weight: bold;" + "-fx-font-size: 14px;");
          if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
          }
        });
  }

  private void showAuctionClosed(String message) {
    if (auctionClosedShown) {
      return;
    }
    auctionClosedShown = true;
    AlertUtils.showInfo("Kết thúc", message);
    timeLabel.setText("Phiên đấu giá đã kết thúc!");
    timeLabel.setStyle("-fx-text-fill: red;" + "-fx-font-weight: bold;" + "-fx-font-size: 14px;");
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
  }

  /** Member. */
  @FXML
  public void handlePlaceBid(ActionEvent event) {
    if (bidLoading) {
      return;
    }
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }
    if (auction == null) {
      AlertUtils.showError("Lỗi", "Phiên không trong thời gian đặt giá");
      return;
    }
    if (UserManager.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để trả giá!");
      return;
    }
    long bidAmount;
    try {
      bidAmount = Long.parseLong(bidAmountField.getText().trim());
    } catch (NumberFormatException e) {
      AlertUtils.showError("Lỗi", "Giá đấu không hợp lệ");
      return;
    }
    if (bidAmount <= currentPrice) {
      AlertUtils.showError("Lỗi", "Giá đấu phải lớn hơn giá hiện tại");
      return;
    }
    BigDecimal available =
        UserManager.getInstance().getCurrentUser().getWallet().getAvailableBalance();
    if (available != null && available.compareTo(BigDecimal.valueOf(bidAmount)) < 0) {
      AlertUtils.showError("Lỗi", "Số dư khả dụng không đủ để đặt giá");
      return;
    }
    try {
      bidButton = LoadingButton.fromEvent(event);
      setBidLoading(true);
      requests.placeBid(auction.id(), bidAmount);
    } catch (IOException e) {
      setBidLoading(false);
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void setBidLoading(boolean loading) {
    bidLoading = loading;
    if (loading) {
      stopBidLoading = LoadingButton.show(bidButton);
    } else {
      stopBidLoading.run();
      stopBidLoading = () -> {};
    }
  }

  private void startCountdownTimer(LocalDateTime endTime) {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r);
              t.setDaemon(true);
              return t;
            });
    scheduler.scheduleAtFixedRate(
        () -> {
          Platform.runLater(
              () -> {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(endTime)) {
                  if (!resultRequested) {
                    resultRequested = true;
                  }
                  scheduler.shutdownNow();
                } else {
                  updateCountdownLabel(now, endTime);
                }
              });
        },
        0,
        1,
        TimeUnit.SECONDS);
  }

  private void requestAuctionResult() {
    if (auction == null) {
      return;
    }
    try {
      requests.fetchAuctionResult(auction.id());
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void updateCountdownLabel(LocalDateTime now, LocalDateTime endTime) {
    long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
    long days = totalSeconds / 86400;
    long hours = (totalSeconds % 86400) / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    timeLabel.setText(String.format("%d Ngày %02d:%02d:%02d", days, hours, minutes, seconds));
  }

  @Override
  public void cleanup() {
    if (cleanedUp) {
      return;
    }
    cleanedUp = true;
    logger.info("Cleaning up LiveController");
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    notifications.removeUpdateListener(updateListener);
    notifications.removeMessageListener(messageListener);
    setBidLoading(false);
    resultRequested = false;
    auctionClosedShown = false;
    auctionDetail = null;
    auction = null;
  }

  /** Member. */
  @FXML
  public void switchToUi(ActionEvent event) {
    cleanup();
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
