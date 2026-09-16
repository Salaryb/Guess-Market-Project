package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("recources/Main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1300, 900);

        //scene.getStylesheets().add(getClass().getResource("/resources/css/light-theme.css").toExternalForm());

        primaryStage.setTitle("Guess Market - Trading System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}