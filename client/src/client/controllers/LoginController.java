package client.controllers;

import client.http.Endpoints;
import client.http.ServerApi;
import client.util.UiUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Map;
import java.util.function.Consumer;

/** The login screen: only a unique user name is needed (no password, by design). */
public class LoginController {
    @FXML private TextField userNameField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private Consumer<String> onLoggedIn;

    public void setOnLoggedIn(Consumer<String> onLoggedIn) {
        this.onLoggedIn = onLoggedIn;
    }

    @FXML
    private void initialize() {
        errorLabel.setText("");
        loginButton.disableProperty().bind(userNameField.textProperty().isEmpty());
    }

    @FXML
    private void onLogin() {
        String userName = userNameField.getText().trim();
        if (userName.isEmpty()) {
            errorLabel.setText("Please enter a user name.");
            return;
        }
        loginButton.disableProperty().unbind();
        loginButton.setDisable(true);
        errorLabel.setText("Connecting...");

        UiUtils.whenDone(ServerApi.post(Endpoints.LOGIN, Map.of("username", userName)),
                registeredName -> onLoggedIn.accept(registeredName),
                error -> {
                    errorLabel.setText(error);
                    loginButton.disableProperty().bind(userNameField.textProperty().isEmpty());
                });
    }
}
