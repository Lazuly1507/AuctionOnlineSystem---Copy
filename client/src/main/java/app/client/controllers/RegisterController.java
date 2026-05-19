package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.RegisterRequest;
import app.common.enums.UserRole;
import app.common.enums.View;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

/** RegisterController. */
public class RegisterController implements Cleanable {
  @FXML private AnchorPane rootPane;
  @FXML private Label lblLogin;
  @FXML private TextField txtName;
  @FXML private TextField txtAccount;
  @FXML private PasswordField txtPassword;
  @FXML private RadioButton rbSeller;
  @FXML private RadioButton rbBidder;
  private final ToggleGroup roleGroup = new ToggleGroup();
  @FXML private Button registerButton;
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private boolean registerLoading;
  private Runnable stopRegisterLoading = () -> {};
  private final Consumer<String> registerListener =
      message -> Platform.runLater(() -> handleRegisterResult(message));

  @FXML
  private void initialize() {
    rbSeller.setToggleGroup(roleGroup);
    rbBidder.setToggleGroup(roleGroup);
    notifications.addMessageListener(registerListener);
    rbBidder.setSelected(true);
    try {
      String url =
          Objects.requireNonNull(getClass().getResource("/app/views/images/background_login.png"))
              .toExternalForm();
      if (rootPane != null) {
        rootPane.setStyle(
            "-fx-background-image: url('"
                + url
                + "');"
                + "-fx-background-size: cover;"
                + "-fx-background-position: center center;"
                + "-fx-background-repeat: no-repeat;"
                + "-fx-background-color: #0a0f16;");
      }
    } catch (Exception e) {
      System.err.println("Không load được background: " + e.getMessage());
    }
  }

  /** Member. */
  @FXML
  public void handleRegister(ActionEvent event) {
    if (registerLoading) {
      return;
    }
    String name = txtName.getText();
    String account = txtAccount.getText();
    String password = txtPassword.getText();
    if (name == null
        || name.trim().isEmpty()
        || account == null
        || account.trim().isEmpty()
        || password == null
        || password.trim().isEmpty()) {
      AlertUtils.showError("Lỗi đăng ký", "Vui lòng nhập đầy đủ Name, Account và Password!");
      return;
    }
    UserRole role = rbSeller.isSelected() ? UserRole.SELLER : UserRole.BIDDER;
    RegisterRequest request = new RegisterRequest(name, account, password, role);
    try {
      setRegisterLoading(true);
      requests.register(request);
    } catch (IOException e) {
      setRegisterLoading(false);
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void handleRegisterResult(String message) {
    if (!registerLoading) {
      return;
    }
    setRegisterLoading(false);
    if (message == null || message.isBlank()) {
      return;
    }
    String lower = message.toLowerCase();
    if (lower.contains("lỗi") || lower.contains("thất bại")) {
      AlertUtils.showError("Lỗi đăng ký", message);
      return;
    }
    AlertUtils.showInfo("Đăng ký", message);
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }

  private void setRegisterLoading(boolean loading) {
    registerLoading = loading;
    if (loading) {
      stopRegisterLoading = LoadingButton.show(registerButton);
    } else {
      stopRegisterLoading.run();
      stopRegisterLoading = () -> {};
    }
  }

  /** Member. */
  @FXML
  public void backToLoginMouse(MouseEvent event) {
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }

  @Override
  public void cleanup() {
    notifications.removeMessageListener(registerListener);
    setRegisterLoading(false);
  }
}
