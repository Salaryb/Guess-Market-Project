package client.refresh;

import client.http.Endpoints;
import client.http.ServerApi;
import client.http.ServerException;
import com.google.gson.reflect.TypeToken;
import dto.ChatDTO;
import dto.ChatLineDTO;
import dto.EventDTO;
import dto.EventDetailsDTO;
import dto.UserDetailsDTO;
import dto.UserEventDetailsDTO;
import dto.UserSummaryDTO;
import javafx.application.Platform;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Pulls data from the server in the background (every 0.5 second).
 * To avoid pulling everything all the time, it first asks for the data version, and fetches the
 * data only when the version changed (or when the user selected another event).
 * The chat is pulled with delta fetching: only lines the client does not have yet.
 * Runs on its own single thread with synchronous requests; results go to the UI with Platform.runLater.
 */
public class DataRefresher {
    private static final long INTERVAL_MILLIS = 500;
    private static final Type EVENTS_TYPE = new TypeToken<List<EventDTO>>() { }.getType();
    private static final Type USERS_TYPE = new TypeToken<List<UserSummaryDTO>>() { }.getType();

    private final Consumer<Snapshot> onData;
    private final Consumer<List<ChatLineDTO>> onChatLines;
    private final Consumer<String> onConnectionStatus;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "data-refresher");
        thread.setDaemon(true);
        return thread;
    });

    private volatile String selectedEvent;
    private volatile String selectedUserEvent;
    private volatile boolean refreshRequested = true;
    private long lastVersion = -1;
    private int chatVersion = 0;
    private boolean connectionLost = false;

    public DataRefresher(Consumer<Snapshot> onData, Consumer<List<ChatLineDTO>> onChatLines,
                         Consumer<String> onConnectionStatus) {
        this.onData = onData;
        this.onChatLines = onChatLines;
        this.onConnectionStatus = onConnectionStatus;
    }

    public void start() {
        executor.scheduleWithFixedDelay(this::tick, 0, INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }

    public void setSelectedEvent(String eventName) {
        selectedEvent = eventName;
        refreshNow();
    }

    public void setSelectedUserEvent(String eventName) {
        selectedUserEvent = eventName;
        refreshNow();
    }

    /** Asks for a full refresh as soon as possible (for example right after the user did an action). */
    public void refreshNow() {
        refreshRequested = true;
        if (!executor.isShutdown()) {
            executor.execute(this::tick);
        }
    }

    private void tick() {
        try {
            pullData();
            pullChat();
            reportConnection(null);
        } catch (ServerException e) {
            reportConnection(e.getMessage());
        } catch (RuntimeException e) {
            reportConnection("Refresh failed: " + e.getMessage());
        }
    }

    private void pullData() {
        long version = ServerApi.get(Endpoints.VERSION, Long.class);
        if (version == lastVersion && !refreshRequested) {
            return;
        }
        refreshRequested = false;
        lastVersion = version;

        List<EventDTO> events = ServerApi.get(Endpoints.EVENTS, EVENTS_TYPE);
        List<UserSummaryDTO> users = ServerApi.get(Endpoints.USERS, USERS_TYPE);
        UserDetailsDTO me = ServerApi.get(Endpoints.USER_DETAILS, UserDetailsDTO.class);
        EventDetailsDTO eventDetails = fetchIfSelected(selectedEvent, Endpoints.EVENT_DETAILS, EventDetailsDTO.class);
        UserEventDetailsDTO userEventDetails =
                fetchIfSelected(selectedUserEvent, Endpoints.USER_EVENT_DETAILS, UserEventDetailsDTO.class);

        Snapshot snapshot = new Snapshot(events, users, me, eventDetails, userEventDetails);
        Platform.runLater(() -> onData.accept(snapshot));
    }

    private <T> T fetchIfSelected(String eventName, String endpoint, Class<T> type) {
        return eventName == null ? null : ServerApi.get(endpoint, Map.of("event", eventName), type);
    }

    private void pullChat() {
        ChatDTO chat = ServerApi.get(Endpoints.CHAT, Map.of("from", String.valueOf(chatVersion)), ChatDTO.class);
        if (!chat.lines().isEmpty()) {
            chatVersion = chat.version();
            Platform.runLater(() -> onChatLines.accept(chat.lines()));
        }
    }

    private void reportConnection(String errorMessage) {
        boolean lost = errorMessage != null;
        if (lost || connectionLost) {
            connectionLost = lost;
            Platform.runLater(() -> onConnectionStatus.accept(errorMessage));
        }
    }
}
