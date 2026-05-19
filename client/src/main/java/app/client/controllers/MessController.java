package app.client.controllers;

import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.utils.AlertUtils;
import app.common.dto.ChatRequest;
import app.common.enums.View;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** MessController. */
public class MessController {
  @FXML private TextArea myTextArea;
  @FXML private VBox chatBox;
  @FXML private ScrollPane scrollPane;
  private final ClientRequestService requests = ClientRequestService.getInstance();

  /** Member. */
  @FXML
  public void initialize() {
    chatBox.heightProperty().addListener((obs, oldVal, newVal) -> scrollPane.setVvalue(1.0d));
  }

  boolean isMe(int id) {
    return UserManager.getInstance().getCurrentUser().getId() == id;
  }

  /** addBubble. */
  public void addBubble(String sender, String content, boolean isMe) {
    HBox row = new HBox();
    row.setPadding(new Insets(10));
    row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    VBox messageGroup = new VBox(4);
    messageGroup.setMaxWidth(520);
    messageGroup.setAlignment(isMe ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
    Label nameLabel = new Label(isMe ? "Bạn" : sender);
    nameLabel.setStyle(
        "-fx-text-fill: #94a3b8;" + "-fx-font-size: 11px;" + "-fx-font-weight: bold;");
    TextFlow bubble = getTextFlow(isMe);
    String[] words = content.split(" ");
    for (String word : words) {
      Text t = new Text(word + " ");
      t.setFont(Font.font("Segoe UI", 14));
      t.setFill(Color.WHITE);
      if (word.startsWith("@")) {
        t.setFill(Color.web("#fbbf24"));
        t.setStyle("-fx-font-weight: bold; -fx-underline: true;");
      }
      bubble.getChildren().add(t);
    }
    messageGroup.getChildren().addAll(nameLabel, bubble);
    row.getChildren().add(messageGroup);
    chatBox.getChildren().add(row);
  }

  private static TextFlow getTextFlow(boolean isMe) {
    TextFlow bubble = new TextFlow();
    bubble.setPadding(new Insets(10, 14, 10, 14));
    if (isMe) {
      bubble.setStyle(
          "-fx-background-color: #4f46e5;"
              + "-fx-background-radius: 14 14 4 14;"
              + "-fx-effect: dropshadow(gaussian, rgba(79,70,229,0.25), 10, 0, 0, 3);");
    } else {
      bubble.setStyle(
          "-fx-background-color: #161b26;"
              + "-fx-background-radius: 14 14 14 4;"
              + "-fx-border-color: #2d3748;"
              + "-fx-border-radius: 14 14 14 4;");
    }
    return bubble;
  }

  /** Member. */
  @FXML
  public void send() {
    String text = myTextArea.getText();
    if (text != null && !text.trim().isEmpty()) {
      ChatRequest chatRequest = new ChatRequest(text);
      try {
        requests.chat(chatRequest);
      } catch (IOException e) {
        AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
        return;
      }
      myTextArea.clear();
    }
  }

  /** Member. */
  @FXML
  public void switchToUi(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
