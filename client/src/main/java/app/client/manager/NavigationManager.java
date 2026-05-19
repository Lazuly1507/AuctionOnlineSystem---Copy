package app.client.manager;

import app.client.controllers.Cleanable;
import app.common.enums.View;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** NavigationManager. */
public class NavigationManager {
  private static final NavigationManager instance = new NavigationManager();
  private final Logger logger = LoggerFactory.getLogger(NavigationManager.class);
  private Stage primaryStage;
  private Object currentController;

  private NavigationManager() {}

  public static NavigationManager getInstance() {
    return instance;
  }

  public void setPrimaryStage(Stage stage) {
    this.primaryStage = stage;
  }

  /** navigateTo. */
  public void navigateTo(View view) {
    navigateTo(view, null);
  }

  /** navigateTo. */
  public void navigateTo(View view, Consumer<Object> controllerCallback) {
    try {
      if (currentController instanceof Cleanable cleanable) {
        cleanable.cleanup();
      }
      FXMLLoader loader = new FXMLLoader(getClass().getResource(view.getFxmlPath()));
      Parent root = loader.load();
      Object newController = loader.getController();
      if (controllerCallback != null) {
        controllerCallback.accept(newController);
      }
      currentController = newController;
      Scene scene = new Scene(root);
      String css =
          Objects.requireNonNull(getClass().getResource("/app/views/style.css")).toExternalForm();
      scene.getStylesheets().add(css);
      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (IOException e) {
      logger.warn("Lỗi nghiêm trọng: Không thể load màn hình " + view.name());
      e.printStackTrace();
    }
  }
}
