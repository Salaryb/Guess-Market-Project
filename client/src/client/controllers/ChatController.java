package client.controllers;

import client.http.Endpoints;
import client.http.ServerApi;
import client.util.UiUtils;
import dto.ChatLineDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Map;

/** Bonus: a chat shared by all logged-in users (everything goes through the server). */
public class ChatController {
    @FXML private ListView<String> chatList;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    public void init() {
        sendButton.disableProperty().bind(messageField.textProperty().isEmpty());
    }

    public void addLines(List<ChatLineDTO> lines) {
        lines.forEach(line -> chatList.getItems().add("[" + line.time() + "] " + line.userName() + ": " + line.text()));
        chatList.scrollTo(chatList.getItems().size() - 1);
    }

    @FXML
    private void onSend() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        UiUtils.whenDone(ServerApi.post(Endpoints.CHAT, Map.of("message", message)),
                result -> messageField.clear(),
                UiUtils::showError);
    }
}
