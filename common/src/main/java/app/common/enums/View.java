package app.common.enums;

/** View. */
public enum View {
  CONNECT("/app/views/ConnectServerController.fxml"),
  LOGIN("/app/views/login_scene.fxml"),
  ORGANIZE("/app/views/hold_an_auction.fxml"),
  LIVE("/app/views/live_auction.fxml"),
  HISTORY("/app/views/my_history.fxml"),
  REGISTER("/app/views/register_account.fxml"),
  UI("/app/views/firstscene.fxml"),
  MESSAGE("/app/views/mess_chat.fxml"),
  USER_PROFILE("/app/views/user_profile.fxml"),
  DEPOSIT("/app/views/deposit.fxml"),
  ALL_AUCTIONS("/app/views/All_Auctions.fxml"),
  ;
  private final String fxmlPath;

  View(String fxmlPath) {
    this.fxmlPath = fxmlPath;
  }

  public String getFxmlPath() {
    return fxmlPath;
  }
}
