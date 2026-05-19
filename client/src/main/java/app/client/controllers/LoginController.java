package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.LoginRequest;
import app.common.enums.View;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** LoginController. */
public class LoginController implements Cleanable {
  @FXML private TextField account;
  @FXML private PasswordField password;
  @FXML private Button loginButton;
  @FXML private Label lblRegister;
  @FXML private AnchorPane rootPane;
  private Logger logger = LoggerFactory.getLogger(LoginController.class);
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private boolean loginLoading;
  private Runnable stopLoginLoading = () -> {};
  private final Consumer<String> loginListener =
      message -> Platform.runLater(() -> handleLoginResult(message));

  @FXML
  private void initialize() {
    String url =
        Objects.requireNonNull(getClass().getResource("/app/views/images/background_login.png"))
            .toExternalForm();
    rootPane.setStyle(
        "-fx-background-image: url('"
            + url
            + "');"
            + "-fx-background-size: cover;"
            + "-fx-background-position: center center;"
            + "-fx-background-repeat: no-repeat;");
    notifications.addMessageListener(loginListener);
  }

  /** Member. */
  @FXML
  public void handleLogin() {
    if (loginLoading) {
      return;
    }
    String userInput = account.getText();
    String passInput = password.getText();
    if (userInput.isEmpty() || passInput.isEmpty()) {
      showAlert();
      return;
    }
    LoginRequest loginRequest = new LoginRequest(userInput, passInput);
    try {
      setLoginLoading(true);
      requests.login(loginRequest);
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
      setLoginLoading(false);
    }
  }

  private void handleLoginResult(String message) {
    if (!loginLoading) {
      return;
    }
    setLoginLoading(false);
    if (UserManager.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Đăng nhập thất bại", message);
      return;
    }
    switchToUi();
  }

  private void setLoginLoading(boolean loading) {
    loginLoading = loading;
    if (loading) {
      stopLoginLoading = LoadingButton.show(loginButton);
    } else {
      stopLoginLoading.run();
      stopLoginLoading = () -> {};
    }
  }

  /** Member. */
  @FXML
  public void switchToUi() {
    try {
      requests.fetchAuctionSummaries();
    } catch (IOException e) {
      AlertUtils.showError("Lỗi", e.getMessage());
    }
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  private void showAlert() {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Cảnh báo");
    alert.setHeaderText(null);
    alert.setContentText("Vui lòng nhập đầy đủ account và Password!");
    alert.showAndWait();
  }

  /** Member. */
  @FXML
  public void handleMouseEntered(MouseEvent event) {
    lblRegister.setUnderline(true);
  }

  /** Member. */
  @FXML
  public void handleMouseExited(MouseEvent event) {
    lblRegister.setUnderline(false);
  }

  /** Member. */
  @FXML
  public void switchToRegister(MouseEvent event) {
    NavigationManager.getInstance().navigateTo(View.REGISTER);
  }

  @Override
  public void cleanup() {
    notifications.removeMessageListener(loginListener);
    setLoginLoading(false);
  }
}
