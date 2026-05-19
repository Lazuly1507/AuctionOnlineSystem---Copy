package app.client.utils;

import javafx.scene.control.Alert;

/** AlertUtils. */
public class AlertUtils {
  /** showError. */
  public static void showError(String header, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Lỗi");
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.showAndWait();
  }

  /** showInfo. */
  public static void showInfo(String header, String content) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Thông báo");
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
