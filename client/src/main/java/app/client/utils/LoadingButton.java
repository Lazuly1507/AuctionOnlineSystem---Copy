package app.client.utils;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ProgressIndicator;

/** Small helper for showing an indeterminate spinner inside a button. */
public final class LoadingButton {
  private LoadingButton() {}

  /** fromEvent. */
  public static Button fromEvent(ActionEvent event) {
    if (event == null || !(event.getSource() instanceof Button button)) {
      return null;
    }
    return button;
  }

  /** show. */
  public static Runnable show(Button button) {
    if (button == null) {
      return () -> {};
    }
    final String oldText = button.getText();
    final Node oldGraphic = button.getGraphic();
    final ContentDisplay oldDisplay = button.getContentDisplay();
    final double oldMinWidth = button.getMinWidth();
    final double width = button.getWidth();
    if (width > 0) {
      button.setMinWidth(Math.max(oldMinWidth, width));
    }

    ProgressIndicator spinner = new ProgressIndicator();
    spinner.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    spinner.setPrefSize(18, 18);
    spinner.setMaxSize(18, 18);
    spinner.setStyle("-fx-progress-color: white;");

    button.setText("");
    button.setGraphic(spinner);
    button.setContentDisplay(ContentDisplay.CENTER);
    return () -> {
      button.setText(oldText);
      button.setGraphic(oldGraphic);
      button.setContentDisplay(oldDisplay);
      button.setMinWidth(oldMinWidth);
    };
  }
}
