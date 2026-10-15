package client.controllers;

import client.http.Endpoints;
import client.http.ServerApi;
import client.refresh.DataRefresher;
import client.refresh.Snapshot;
import client.util.Format;
import client.util.UiUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.Map;

/**
 * The main screen: header (title, user, balance, logout) and the tabs (Events, Account, Chat).
 * Owns the DataRefresher and hands every new snapshot of data to the tab controllers.
 */
public class MainController {
    @FXML private Label userLabel;
    @FXML private Label balanceLabel;
    @FXML private Label statusLabel;

    // Controllers of the included tab FXMLs (fx:id + "Controller")
    @FXML private EventsController eventsTabController;
    @FXML private AccountController accountTabController;
    @FXML private ChatController chatTabController;

    private DataRefresher refresher;
    private Runnable onLoggedOut;

    public void start(String userName, Runnable onLoggedOut) {
        this.onLoggedOut = onLoggedOut;
        userLabel.setText("Logged in as: " + userName);
        statusLabel.setText("");

        refresher = new DataRefresher(this::onSnapshot, chatTabController::addLines, this::onConnectionStatus);
        eventsTabController.init(refresher);
        accountTabController.init(userName, refresher);
        chatTabController.init();
        refresher.start();
    }

    private void onSnapshot(Snapshot snapshot) {
        balanceLabel.setText("Balance: " + Format.money(snapshot.me().balance()));
        eventsTabController.update(snapshot);
        accountTabController.update(snapshot);
    }

    private void onConnectionStatus(String error) {
        statusLabel.setText(error == null ? "" : error);
        statusLabel.setStyle(error == null ? "" : "-fx-text-fill: #dc2626;");
    }

    @FXML
    private void onLogout() {
        refresher.stop();
        UiUtils.whenDone(ServerApi.post(Endpoints.LOGOUT, Map.of()),
                message -> onLoggedOut.run(),
                error -> onLoggedOut.run());
    }

    /** Called when the window is closed. */
    public void shutdown() {
        refresher.stop();
        ServerApi.postAndWait(Endpoints.LOGOUT);
    }
}
