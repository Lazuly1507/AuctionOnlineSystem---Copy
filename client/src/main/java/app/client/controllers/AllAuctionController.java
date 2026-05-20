package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.store.AuctionStore;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.AuctionSummary;
import app.common.enums.AuctionStatus;
import app.common.enums.View;
import java.util.ArrayList;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** AllAuctionController. */
public class AllAuctionController implements Cleanable {
  private static final double CARD_WIDTH = 260;
  private static final double CARD_HEIGHT = 300;
  @FXML private FlowPane runningPane;
  @FXML private ComboBox<String> typeFilterComboBox;
  @FXML private TextField searchField;
  @FXML private FlowPane finishedPane;
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private final AuctionStore store = AuctionStore.getInstance();
  private final List<AuctionSummary> summaries = new ArrayList<>();
  private boolean reloadLoading;
  private Button reloadButton;
  private Runnable stopReloadLoading = () -> {};
  private final Runnable summariesListener =
      () -> {
        requestAuctions();
        rebuildUi();
        setReloadLoading(false);
      };

  /** Member. */
  @FXML
  public void initialize() {
    typeFilterComboBox.getItems().addAll("ALL", "ELECTRONICS", "ART", "VEHICLE");
    typeFilterComboBox.setValue("ALL");
    typeFilterComboBox.setOnAction(e -> rebuildUi());
    searchField.textProperty().addListener((obs, oldValue, newValue) -> rebuildUi());
    notifications.addUpdateListener(summariesListener);
    requestAuctions();
    rebuildUi();
  }

  private void requestAuctions() {
    summaries.clear();
    summaries.addAll(store.getAuctionSummaries());
  }

  private void rebuildUi() {
    if (runningPane == null || finishedPane == null) {
      return;
    }
    runningPane.getChildren().clear();
    finishedPane.getChildren().clear();
    String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
    String type = typeFilterComboBox.getValue() == null ? "ALL" : typeFilterComboBox.getValue();
    for (AuctionSummary summary : summaries) {
      if (!matchFilter(summary, query, type)) {
        continue;
      }
      if (summary.status() == AuctionStatus.RUNNING) {
        runningPane.getChildren().add(createAuctionCard(summary));
      } else {
        finishedPane.getChildren().add(createAuctionCard(summary));
      }
    }
  }

  private boolean matchFilter(AuctionSummary summary, String query, String type) {
    if (summary == null) {
      return false;
    }
    String name = summary.itemName() == null ? "" : summary.itemName();
    if (!query.isBlank() && !name.toLowerCase().contains(query)) {
      return false;
    }
    return "ALL".equals(type) || name.toUpperCase().contains(type);
  }

  private VBox createAuctionCard(AuctionSummary summary) {
    VBox vbox = new VBox();
    vbox.setPrefWidth(CARD_WIDTH);
    vbox.setMinWidth(CARD_WIDTH);
    vbox.setMaxWidth(CARD_WIDTH);
    vbox.setPrefHeight(CARD_HEIGHT);
    vbox.setStyle(
        "-fx-background-color: #1a1f35;"
            + "-fx-background-radius: 8;"
            + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);"
            + "-fx-padding: 15;"
            + "-fx-spacing: 10;");
    StackPane imagePane = new StackPane();
    imagePane.setPrefHeight(100);
    imagePane.setStyle("-fx-background-color: #2a2f45;" + "-fx-background-radius: 5;");
    Label imgLabel = new Label("Ảnh tài sản");
    imgLabel.setStyle("-fx-text-fill: #aaa;");
    imagePane.getChildren().add(imgLabel);
    Label titleLabel = new Label(summary.itemName());
    titleLabel.setWrapText(true);
    titleLabel.setStyle(
        "-fx-font-weight: bold;" + "-fx-font-size: 14px;" + "-fx-text-fill: white;");
    Label priceLabel = new Label("Giá hiện tại: " + summary.currentPrice() + " đ");
    priceLabel.setStyle("-fx-text-fill: #e91e63;" + "-fx-font-weight: bold;");
    Label timeLabel = new Label("Kết thúc: " + summary.endTime());
    timeLabel.setStyle("-fx-text-fill: #9aa0b4;" + "-fx-font-size: 12px;");
    Button btnDetail =
        new Button(summary.status() == AuctionStatus.FINISHED ? "Xem kết quả" : "Chi tiết");
    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.setStyle(
        "-fx-background-color: #673ab7;" + "-fx-text-fill: white;" + "-fx-cursor: hand;");
    btnDetail.setOnAction(e -> NavigationManager.getInstance().openAuctionDetail(summary));
    vbox.getChildren().addAll(imagePane, titleLabel, priceLabel, timeLabel, btnDetail);
    return vbox;
  }

  /** Member. */
  @FXML
  public void handleReload(ActionEvent event) {
    if (reloadLoading) {
      return;
    }
    try {
      reloadButton = LoadingButton.fromEvent(event);
      setReloadLoading(true);
      requests.fetchAuctionSummaries();
    } catch (Exception e) {
      setReloadLoading(false);
      AlertUtils.showError("Lỗi", e.getMessage());
    }
  }

  private void setReloadLoading(boolean loading) {
    reloadLoading = loading;
    if (loading) {
      stopReloadLoading = LoadingButton.show(reloadButton);
    } else {
      stopReloadLoading.run();
      stopReloadLoading = () -> {};
    }
  }

  /** Member. */
  @FXML
  public void switchToUi() {
    try {
      NavigationManager.getInstance().navigateTo(View.UI);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void cleanup() {
    notifications.removeUpdateListener(summariesListener);
    setReloadLoading(false);
  }
}
