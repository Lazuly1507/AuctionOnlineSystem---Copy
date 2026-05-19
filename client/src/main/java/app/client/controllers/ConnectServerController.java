package app.client.controllers;

import app.client.Client;
import app.client.manager.NavigationManager;
import app.client.utils.LoadingButton;
import app.common.enums.View;
import app.common.exception.AppException;
import java.io.IOException;
import java.util.Objects;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

/** ConnectServerController. */
public class ConnectServerController {
  @FXML private Button connectButton;
  @FXML private Label statusLabel;
  @FXML private AnchorPane rootPane;
  private static final int CONNECTION_TIMEOUT = 5000;
  private boolean connecting;
  private Runnable stopConnectionLoading = () -> {};

  @FXML
  private void initialize() {
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
  public void connectServer(ActionEvent event) {
    if (connecting) {
      return;
    }
    if (connectButton != null) {
      connecting = true;
      stopConnectionLoading = LoadingButton.show(connectButton);
    }
    if (statusLabel != null) {
      statusLabel.setText("Đang kết nối tới Server...");
      statusLabel.setStyle("-fx-text-fill: #FFA500;");
    }
    Thread connectionThread =
        new Thread(
            () -> {
              try {
                Client connectedClient = connectWithTimeout();
                if (connectedClient != null) {
                  Platform.runLater(
                      () -> {
                        if (statusLabel != null) {
                          statusLabel.setText("✓ Kết nối thành công!");
                          statusLabel.setStyle("-fx-text-fill: #00AA00;");
                        }
                        resetConnectionButton();
                        try {
                          handleLoginClick(event);
                        } catch (IOException e) {
                          throw new AppException("Lỗi");
                        }
                      });
                } else {
                  throw new IOException(
                      "Timeout: Không thể kết nối trong " + CONNECTION_TIMEOUT + "ms");
                }
              } catch (IOException e) {
                Platform.runLater(
                    () -> {
                      String errorMsg = getDetailedErrorMessage(e);
                      showAlert("Lỗi kết nối", errorMsg);
                      if (statusLabel != null) {
                        statusLabel.setText("✗ Kết nối thất bại");
                        statusLabel.setStyle("-fx-text-fill: #FF0000;");
                      }
                      resetConnectionButton();
                    });
              } catch (Exception e) {
                Platform.runLater(
                    () -> {
                      showAlert("Lỗi không xác định", e.getMessage());
                      resetConnectionButton();
                    });
              }
            });
    connectionThread.setDaemon(true);
    connectionThread.start();
  }

  private Client connectWithTimeout() throws IOException, InterruptedException {
    final Client[] result = {null};
    final Exception[] exception = {null};
    Thread connectionAttempt =
        new Thread(
            () -> {
              try {
                result[0] = Client.getInstance();
                result[0].connect();
              } catch (Exception e) {
                exception[0] = e;
              }
            });
    connectionAttempt.setDaemon(true);
    connectionAttempt.start();
    connectionAttempt.join(CONNECTION_TIMEOUT);
    if (exception[0] != null) {
      throw new IOException(exception[0]);
    }
    return result[0];
  }

  private String getDetailedErrorMessage(Exception e) {
    String message = e.getMessage();
    if (message == null || message.isEmpty()) {
      message = e.getClass().getSimpleName();
    }
    if (message.contains("Connection refused")) {
      return "Server không phản hồi. Vui lòng kiểm tra:\n"
          + "- Server đã khởi động chưa?\n"
          + "- Địa chỉ IP và cổng có đúng không?";
    } else if (message.contains("Timeout")) {
      return "Hết thời gian chờ kết nối. Server không phản hồi.";
    } else if (message.contains("UnknownHostException")) {
      return "Không tìm thấy Server. Kiểm tra địa chỉ IP.";
    } else if (message.contains("Network is unreachable")) {
      return "Không thể truy cập mạng. Kiểm tra kết nối Internet.";
    }
    return "Lỗi kết nối: " + message;
  }

  private void resetConnectionButton() {
    connecting = false;
    stopConnectionLoading.run();
    stopConnectionLoading = () -> {};
    if (connectButton != null) {
      connectButton.setText("Kết nối");
    }
  }

  private void showAlert(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  /** Member. */
  @FXML
  public void handleLoginClick(ActionEvent event) throws IOException {
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }
}
