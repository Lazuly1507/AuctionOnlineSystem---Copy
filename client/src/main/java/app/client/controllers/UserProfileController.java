package app.client.controllers;

import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.utils.AlertUtils;
import app.common.enums.View;
import app.common.models.User;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** UserProfileController. */
public class UserProfileController {
  @FXML private AnchorPane rootPane;
  @FXML private Circle avatarCircle;
  @FXML private Label avatarLabel;
  @FXML private Label userNameLabel;
  @FXML private Label emailLabel;
  @FXML private Label walletLabel;
  @FXML private Label roleLabel;
  @FXML private Label idLabel;
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");

  /** Member. */
  @FXML
  public void initialize() {
    loadUserProfile();
  }

  private void loadUserProfile() {
    User currentUser = UserManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      AlertUtils.showError("Lỗi", "Không tìm thấy thông tin user!");
      try {
        NavigationManager.getInstance().navigateTo(View.LOGIN);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return;
    }
    if (avatarCircle != null) {
      avatarCircle.setFill(Color.web("#673ab7"));
      avatarCircle.setRadius(80);
    }
    if (avatarLabel != null) {
      String firstLetter = currentUser.getName().substring(0, 1).toUpperCase();
      avatarLabel.setText(firstLetter);
    }
    if (userNameLabel != null) {
      userNameLabel.setText(currentUser.getName());
    }
    if (emailLabel != null) {
      emailLabel.setText("📧 " + currentUser.getAccount().getUsername());
    }
    if (idLabel != null) {
      idLabel.setText("ID: #" + currentUser.getId());
    }
    if (roleLabel != null) {
      roleLabel.setText("🎯 " + currentUser.getRole());
    }
    if (walletLabel != null) {
      BigDecimal balance = currentUser.getWallet().getTotalBalance();
      walletLabel.setText(String.format("💰 %s đ", currencyFormat.format(balance)));
      if (balance.compareTo(new BigDecimal("1000000")) < 0) {
        walletLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
      } else {
        walletLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
      }
    }
  }

  /** Member. */
  @FXML
  public void handleLogout(ActionEvent event) {
    try {
      UserManager.getInstance().setCurrentUser(null);
      AuctionStore.getInstance().clearHistory();
      AlertUtils.showInfo("Thành công", "Đã đăng xuất!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
    } catch (Exception e) {
      AlertUtils.showError("Lỗi", "Không thể đăng xuất: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Member. */
  @FXML
  public void handleBack(ActionEvent event) {
    try {
      NavigationManager.getInstance().navigateTo(View.UI);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
