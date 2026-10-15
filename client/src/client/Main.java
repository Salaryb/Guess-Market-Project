package client;

import client.controllers.LoginController;
import client.controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/** The Guess Market JavaFX client: starts with the login screen and moves to the main screen after login. */
public class Main extends Application {
    private static final String LOGIN_FXML = "resources/login.fxml";
    private static final String MAIN_FXML = "resources/main.fxml";
    private static final String CSS = "resources/css/light-theme.css";

    private Stage stage;
    private MainController mainController;

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.stage = primaryStage;
        stage.setTitle("Guess Market");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(new Scene(new javafx.scene.layout.Pane(), 1300, 850));
        stage.getScene().getStylesheets().add(resource(CSS).toExternalForm());
        showLogin();
        stage.show();
    }

    public void showLogin() throws IOException {
        mainController = null;
        FXMLLoader loader = new FXMLLoader(resource(LOGIN_FXML));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setOnLoggedIn(this::showMainScreen);
        stage.getScene().setRoot(root);
        stage.setTitle("Guess Market - Login");
    }

    private void showMainScreen(String userName) {
        try {
            FXMLLoader loader = new FXMLLoader(resource(MAIN_FXML));
            Parent root = loader.load();
            mainController = loader.getController();
            mainController.start(userName, () -> {
                try {
                    showLogin();
                } catch (IOException e) {
                    stage.close();
                }
            });
            stage.getScene().setRoot(root);
            stage.setTitle("Guess Market - " + userName);
        } catch (IOException e) {
            throw new IllegalStateException("Failed loading the main screen", e);
        }
    }

    /** Closing the window logs the user out, so his name is free for the next login. */
    @Override
    public void stop() {
        if (mainController != null) {
            mainController.shutdown();
        }
    }

    private URL resource(String path) {
        return Main.class.getResource(path);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
