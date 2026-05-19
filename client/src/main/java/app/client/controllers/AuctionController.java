package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.CreateAuctionRequest;
import app.common.enums.ItemType;
import app.common.enums.View;
import java.io.IOException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/** AuctionController. */
public class AuctionController implements Cleanable {
  @FXML private TextField nameField;
  @FXML private TextArea descriptionField;
  @FXML private TextField startingPriceField;
  @FXML private TextField stepPriceField;
  @FXML private ComboBox<ItemType> typeComboBox;
  @FXML private TextField durationField;
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private boolean createLoading;
  private Button createButton;
  private Runnable stopCreateLoading = () -> {};
  private final Consumer<String> createAuctionListener =
      message -> Platform.runLater(() -> handleCreateAuctionResult(message));

  /** Member. */
  @FXML
  public void initialize() {
    notifications.addMessageListener(createAuctionListener);
    if (UserManager.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập để tổ chức phiên đấu giá!");
      Platform.runLater(() -> NavigationManager.getInstance().navigateTo(View.LOGIN));
      return;
    }
    if (typeComboBox != null) {
      typeComboBox.getItems().setAll(ItemType.values());
      typeComboBox.getSelectionModel().selectFirst();
    }
  }

  /** Member. */
  @FXML
  public void handleAdd(ActionEvent event) {
    if (createLoading) {
      return;
    }
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server.");
      return;
    }
    if (UserManager.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }
    try {
      String name = nameField.getText();
      String desc = descriptionField.getText();
      long startPrice = Long.parseLong(startingPriceField.getText());
      long stepPrice = Long.parseLong(stepPriceField.getText());
      int durationMins = Integer.parseInt(durationField.getText());
      ItemType type = typeComboBox.getValue();
      if (name.isEmpty() || desc.isEmpty()) {
        AlertUtils.showError("Lỗi", "Thiếu thông tin.");
        return;
      }
      CreateAuctionRequest request =
          new CreateAuctionRequest(name, desc, startPrice, stepPrice, type, durationMins);
      createButton = LoadingButton.fromEvent(event);
      setCreateLoading(true);
      requests.createAuction(request);
    } catch (NumberFormatException e) {
      AlertUtils.showError("Sai định dạng", "Giá / thời gian phải là số");
    } catch (IOException e) {
      setCreateLoading(false);
      AlertUtils.showError("Lỗi", "Server không phản hồi");
    } catch (Exception e) {
      setCreateLoading(false);
      AlertUtils.showError("Lỗi", e.getMessage());
      e.printStackTrace();
    }
  }

  private void handleCreateAuctionResult(String message) {
    if (!createLoading) {
      return;
    }
    setCreateLoading(false);
    if (!isSuccessMessage(message)) {
      AlertUtils.showError("Tạo phiên thất bại", message);
      return;
    }
    AlertUtils.showInfo("Tạo phiên", message);
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  private boolean isSuccessMessage(String message) {
    if (message == null) {
      return false;
    }
    String normalized = message.toLowerCase();
    return normalized.contains("ok") || normalized.contains("thành công");
  }

  private void setCreateLoading(boolean loading) {
    createLoading = loading;
    if (loading) {
      stopCreateLoading = LoadingButton.show(createButton);
    } else {
      stopCreateLoading.run();
      stopCreateLoading = () -> {};
    }
  }

  /** Member. */
  @FXML
  public void handleBack(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  @Override
  public void cleanup() {
    notifications.removeMessageListener(createAuctionListener);
    setCreateLoading(false);
  }
}
