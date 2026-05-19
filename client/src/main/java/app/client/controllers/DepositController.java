package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.enums.View;
import app.common.models.User;
import app.common.models.Wallet;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DepositController. */
public class DepositController implements Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(DepositController.class);
  @FXML private TextField depositAmountField;
  @FXML private Label totalBalanceLabel;
  @FXML private Label availableBalanceLabel;
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
  private boolean depositLoading;
  private Button depositButton;
  private Runnable stopDepositLoading = () -> {};
  private BigDecimal balanceBeforeDeposit;
  private final Consumer<String> walletListener =
      message -> Platform.runLater(() -> handleWalletUpdate(message));

  /** Member. */
  @FXML
  public void initialize() {
    notifications.addMessageListener(walletListener);
    updateBalanceLabels(UserManager.getInstance().getCurrentUser());
  }

  private void updateBalanceLabels(User user) {
    if (totalBalanceLabel == null || availableBalanceLabel == null) {
      return;
    }
    if (user == null) {
      totalBalanceLabel.setText("Tổng số dư: 0 đ");
      availableBalanceLabel.setText("Số dư khả dụng: 0 đ");
      return;
    }
    Wallet wallet = user.getWallet();
    totalBalanceLabel.setText("Tổng số dư: " + formatCurrency(wallet.getTotalBalance()) + " đ");
    availableBalanceLabel.setText(
        "Số dư khả dụng: " + formatCurrency(wallet.getAvailableBalance()) + " đ");
  }

  private String formatCurrency(BigDecimal amount) {
    if (amount == null) {
      return "0";
    }
    return currencyFormat.format(amount);
  }

  /** Member. */
  @FXML
  public void handleDeposit(ActionEvent event) {
    if (depositLoading) {
      return;
    }
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }
    if (UserManager.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để nạp tiền!");
      return;
    }
    if (depositAmountField == null) {
      return;
    }
    BigDecimal amount;
    try {
      amount = new BigDecimal(depositAmountField.getText().trim());
    } catch (NumberFormatException e) {
      AlertUtils.showError("Lỗi", "Số tiền nạp không hợp lệ");
      return;
    }
    if (amount.signum() <= 0) {
      AlertUtils.showError("Lỗi", "Số tiền nạp phải lớn hơn 0");
      return;
    }
    try {
      depositButton = LoadingButton.fromEvent(event);
      balanceBeforeDeposit = currentTotalBalance();
      setDepositLoading(true);
      requests.deposit(amount);
    } catch (Exception e) {
      setDepositLoading(false);
      logger.error("Failed to deposit", e);
      AlertUtils.showError("Lỗi", "Không thể nạp tiền.");
    }
  }

  private void handleWalletUpdate(String message) {
    if (!depositLoading) {
      updateBalanceLabels(UserManager.getInstance().getCurrentUser());
      return;
    }
    setDepositLoading(false);
    updateBalanceLabels(UserManager.getInstance().getCurrentUser());
    if (!hasBalanceChanged() && !isSuccessMessage(message)) {
      AlertUtils.showError("Ví", message);
      return;
    }
    if (depositAmountField != null) {
      depositAmountField.clear();
    }
  }

  private BigDecimal currentTotalBalance() {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null || user.getWallet() == null || user.getWallet().getTotalBalance() == null) {
      return BigDecimal.ZERO;
    }
    return user.getWallet().getTotalBalance();
  }

  private boolean hasBalanceChanged() {
    return balanceBeforeDeposit != null
        && currentTotalBalance().compareTo(balanceBeforeDeposit) > 0;
  }

  private boolean isSuccessMessage(String message) {
    if (message == null) {
      return false;
    }
    String normalized = message.toLowerCase();
    return normalized.contains("ok") || normalized.contains("thành công");
  }

  private void setDepositLoading(boolean loading) {
    depositLoading = loading;
    if (loading) {
      stopDepositLoading = LoadingButton.show(depositButton);
    } else {
      stopDepositLoading.run();
      stopDepositLoading = () -> {};
    }
  }

  /** Member. */
  @FXML
  public void handleBack(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  @Override
  public void cleanup() {
    notifications.removeMessageListener(walletListener);
    setDepositLoading(false);
  }
}
