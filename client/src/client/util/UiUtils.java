package client.util;

import client.http.ServerException;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/** Helpers for showing messages and for handling asynchronous server answers on the JavaFX thread. */
public final class UiUtils {

    private UiUtils() {
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle("Guess Market");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * When the asynchronous request completes, runs onSuccess / onError on the JavaFX thread.
     * The calling (JavaFX) thread does not wait.
     */
    public static <T> void whenDone(CompletableFuture<T> future, Consumer<T> onSuccess, Consumer<String> onError) {
        future.whenComplete((result, error) -> Platform.runLater(() -> {
            if (error == null) {
                onSuccess.accept(result);
            } else {
                onError.accept(messageOf(error));
            }
        }));
    }

    /** Typical action: show the server's message as info on success, or as an error on failure. */
    public static void whenDone(CompletableFuture<String> future, Runnable afterSuccess) {
        whenDone(future, message -> {
            afterSuccess.run();
            showInfo(message);
        }, UiUtils::showError);
    }

    public static String messageOf(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause instanceof ServerException ? cause.getMessage() : "Unexpected error: " + cause.getMessage();
    }
}
