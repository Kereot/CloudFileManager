package gui.client;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Controller.loginWindow(stage);
    }

    public static void main(String[] args) {
        launch();
    }
}