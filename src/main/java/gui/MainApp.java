package gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
  @Override
  public void start(Stage stage) {
    MainController controller = new MainController();
    Scene scene = new Scene(controller.getView(), 1440, 860);
    stage.setTitle("CPU Scheduling Comparator — Round Robin / SJF / SRTF");
    stage.setScene(scene);
    stage.setMinWidth(1180);
    stage.setMinHeight(720);

    stage.setMaximized(true);
    stage.setFullScreen(false);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}

