package client.controllers;

import client.http.Endpoints;
import client.http.ServerApi;
import client.refresh.DataRefresher;
import client.refresh.Snapshot;
import client.util.Format;
import client.util.TableUtils;
import client.util.UiUtils;
import dto.HoldingDTO;
import dto.OptionStatusDTO;
import dto.OrderDTO;
import dto.TradeDTO;
import dto.TransactionDTO;
import dto.UserDetailsDTO;
import dto.UserEventDetailsDTO;
import dto.UserEventSummaryDTO;
import dto.UserSummaryDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The Account tab (laid out as in the exercise 3 sketch): the logged-in user's private screen.
 * Loading event files, the users table, his account details (transactions, balance, loading funds),
 * the events and his role in each, and the selected event's details with the actions he can do in it
 * (activate / close as market maker, buy LMSR shares / place order book orders).
 */
public class AccountController {
    private static final String NO_ROLE = "-";
    private static final String ACTIVE = "Active";
    private static final String NOT_STARTED = "Not Started";
    private static final String LMSR = "LMSR";

    @FXML private Button uploadButton;
    @FXML private Label filePathLabel;
    @FXML private ProgressBar progressBar;

    @FXML private Label balanceLabel;
    @FXML private Label accountStatusLabel;
    @FXML private TextField depositField;
    @FXML private Button depositButton;

    @FXML private TableView<UserSummaryDTO> usersTable;
    @FXML private TableView<UserEventSummaryDTO> userEventsTable;
    @FXML private CheckBox onlyMyEventsCheck;

    @FXML private Label eventPlaceholderLabel;
    @FXML private VBox eventDetailsBox;
    @FXML private Label selectedEventLabel;
    @FXML private Label eventInfoLabel;

    @FXML private VBox marketMakerBox;
    @FXML private Button activateButton;
    @FXML private ComboBox<String> winnerCombo;
    @FXML private Button closeEventButton;

    @FXML private VBox lmsrTradeBox;
    @FXML private ComboBox<String> lmsrOptionCombo;
    @FXML private TextField lmsrSharesField;
    @FXML private Button buyButton;

    @FXML private VBox orderTradeBox;
    @FXML private ComboBox<String> orderSideCombo;
    @FXML private ComboBox<String> orderOptionCombo;
    @FXML private TextField orderSharesField;
    @FXML private TextField orderPriceField;
    @FXML private Button placeOrderButton;

    @FXML private Label commissionPaidLabel;
    @FXML private Label profitLossLabel;
    @FXML private Label winnerLabel;
    @FXML private TableView<HoldingDTO> holdingsTable;
    @FXML private VBox lmsrHistoryBox;
    @FXML private TableView<TradeDTO> myTradesTable;
    @FXML private TableView<OptionStatusDTO> totalsTable;
    @FXML private VBox openOrdersBox;
    @FXML private TableView<OrderDTO> openOrdersTable;

    @FXML private TableView<TransactionDTO> transactionsTable;

    private final ObservableList<UserEventSummaryDTO> allUserEvents = FXCollections.observableArrayList();
    private final FilteredList<UserEventSummaryDTO> shownUserEvents = new FilteredList<>(allUserEvents, e -> true);
    private DataRefresher refresher;
    private String userName;
    private String selectedEventName;
    private UserEventDetailsDTO shownDetails;
    private boolean updatingTable = false;

    public void init(String userName, DataRefresher refresher) {
        this.userName = userName;
        this.refresher = refresher;
        setupTables();
        orderSideCombo.setItems(FXCollections.observableArrayList("Buy", "Sell"));
        orderSideCombo.getSelectionModel().selectFirst();
        progressBar.setVisible(false);
        onlyMyEventsCheck.selectedProperty().addListener((obs, oldValue, onlyMine) ->
                shownUserEvents.setPredicate(e -> !onlyMine || !NO_ROLE.equals(e.role())));
        showEventDetails(null);
    }

    private void setupTables() {
        TableUtils.setColumns(usersTable,
                TableUtils.textColumn("User Name", UserSummaryDTO::name),
                TableUtils.moneyColumn("Balance", UserSummaryDTO::balance),
                TableUtils.textColumn("Market Maker", u -> u.marketMaker() ? "Yes" : "No"),
                TableUtils.textColumn("Online", u -> u.online() ? "Yes" : "No"));
        usersTable.setPlaceholder(new Label("No other users are registered yet."));

        TableUtils.setColumns(userEventsTable,
                TableUtils.textColumn("Event", UserEventSummaryDTO::eventName),
                TableUtils.textColumn("Type", UserEventSummaryDTO::type),
                TableUtils.textColumn("Status", UserEventSummaryDTO::status),
                TableUtils.textColumn("My Role", UserEventSummaryDTO::role));
        userEventsTable.setItems(shownUserEvents);
        userEventsTable.setPlaceholder(new Label("No events."));
        userEventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingTable) {
                onEventSelected(newValue);
            }
        });

        TableUtils.setColumns(holdingsTable,
                TableUtils.textColumn("Option", HoldingDTO::option),
                TableUtils.intColumn("Shares Held", HoldingDTO::shares),
                TableUtils.moneyColumn("Amount Paid", HoldingDTO::amountPaid),
                TableUtils.moneyColumn("Current Value", HoldingDTO::value));
        TableUtils.setColumns(myTradesTable,
                TableUtils.textColumn("Option", TradeDTO::option),
                TableUtils.intColumn("Shares", TradeDTO::shares),
                TableUtils.moneyColumn("Price Paid", TradeDTO::pricePaid),
                TableUtils.moneyColumn("Commission", TradeDTO::commission));
        TableUtils.setColumns(totalsTable,
                TableUtils.textColumn("Option", OptionStatusDTO::option),
                TableUtils.moneyColumn("Current Value", OptionStatusDTO::price),
                TableUtils.intColumn("Total Shares Bought (all users)", OptionStatusDTO::sharesBought));
        TableUtils.setColumns(openOrdersTable,
                TableUtils.textColumn("Side", o -> o.buy() ? "Buy" : "Sell"),
                TableUtils.textColumn("Option", OrderDTO::option),
                TableUtils.intColumn("Shares Left", OrderDTO::shares),
                TableUtils.moneyColumn("Price per Share", OrderDTO::price));
        openOrdersTable.setPlaceholder(new Label("No waiting orders."));

        TableUtils.setColumns(transactionsTable,
                TableUtils.intColumn("#", TransactionDTO::number),
                TableUtils.textColumn("Time", TransactionDTO::time),
                TableUtils.textColumn("Type", TransactionDTO::type),
                TableUtils.textColumn("Description", TransactionDTO::description),
                TableUtils.moneyColumn("Amount", TransactionDTO::amount),
                TableUtils.moneyColumn("Balance Before", TransactionDTO::balanceBefore),
                TableUtils.moneyColumn("Balance After", TransactionDTO::balanceAfter));
        transactionsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        double[] widths = {40, 70, 150, 300, 80, 105, 105};
        for (int i = 0; i < widths.length; i++) {
            transactionsTable.getColumns().get(i).setPrefWidth(widths[i]);
        }
        transactionsTable.setPlaceholder(new Label("No transactions yet. Use 'Load Funds' to add money into your account."));
    }

    // ----- Data updates -----

    public void update(Snapshot snapshot) {
        UserDetailsDTO me = snapshot.me();
        balanceLabel.setText("Balance: " + Format.money(me.balance()));
        accountStatusLabel.setText(me.blocked() ? "BLOCKED (negative balance)" : "Active");
        accountStatusLabel.setStyle(me.blocked() ? "-fx-text-fill: #dc2626;" : "-fx-text-fill: #16a34a;");

        List<UserSummaryDTO> others = snapshot.users().stream()
                .filter(u -> !u.name().equalsIgnoreCase(userName)).toList();
        TableUtils.updateItems(usersTable, usersTable.getItems(), others, UserSummaryDTO::name);

        updatingTable = true;
        TableUtils.updateItems(userEventsTable, allUserEvents, me.events(), UserEventSummaryDTO::eventName);
        updatingTable = false;

        List<TransactionDTO> latestFirst = new ArrayList<>(me.transactions());
        Collections.reverse(latestFirst);
        TableUtils.updateItems(transactionsTable, latestFirst);

        UserEventDetailsDTO details = snapshot.userEventDetails();
        if (details != null && details.eventName().equals(selectedEventName) && !details.equals(shownDetails)) {
            showEventDetails(details);
        }
    }

    private void onEventSelected(UserEventSummaryDTO event) {
        String name = event == null ? null : event.eventName();
        if (name != null && name.equals(selectedEventName)) {
            return;
        }
        selectedEventName = name;
        showEventDetails(null);
        if (name != null) {
            eventPlaceholderLabel.setText("Loading '" + name + "'...");
        }
        refresher.setSelectedUserEvent(name);
    }

    private void showEventDetails(UserEventDetailsDTO details) {
        boolean optionsChanged = details == null || shownDetails == null
                || !details.eventName().equals(shownDetails.eventName());
        shownDetails = details;
        boolean hasDetails = details != null;
        setShown(eventDetailsBox, hasDetails);
        setShown(eventPlaceholderLabel, !hasDetails);
        eventPlaceholderLabel.setText("Select an event to see your involvement in it and to trade.");
        if (!hasDetails) {
            return;
        }

        boolean isLmsr = LMSR.equals(details.type());
        boolean isActive = ACTIVE.equals(details.status());
        selectedEventLabel.setText(details.eventName());
        eventInfoLabel.setText(details.type() + "  |  " + details.status()
                + (details.marketMaker() ? "  |  You are the market maker" : ""));
        if (optionsChanged) {
            resetOptionChoices(details.options());
        }

        setShown(marketMakerBox, details.marketMaker());
        activateButton.setDisable(!NOT_STARTED.equals(details.status()));
        closeEventButton.setDisable(!isActive);
        winnerCombo.setDisable(!isActive);

        setShown(lmsrTradeBox, isLmsr);
        setShown(orderTradeBox, !isLmsr);
        lmsrTradeBox.setDisable(!isActive);
        orderTradeBox.setDisable(!isActive);

        commissionPaidLabel.setText(Format.money(details.commissionPaid()));
        profitLossLabel.setText(details.profitLoss() == null ? "Available when the event ends" : Format.money(details.profitLoss()));
        winnerLabel.setText(details.winningOption() == null ? "-" : details.winningOption());

        TableUtils.updateItems(holdingsTable, details.holdings());
        setShown(lmsrHistoryBox, isLmsr);
        setShown(openOrdersBox, !isLmsr);
        TableUtils.updateItems(myTradesTable, details.trades());
        TableUtils.updateItems(totalsTable, details.totalsPerOption());
        TableUtils.updateItems(openOrdersTable, details.openOrders());
    }

    /** Refills the option choices only when another event is selected, so a refresh never resets the user's choice. */
    private void resetOptionChoices(List<String> options) {
        for (ComboBox<String> combo : List.of(winnerCombo, lmsrOptionCombo, orderOptionCombo)) {
            combo.setItems(FXCollections.observableArrayList(options));
            combo.getSelectionModel().clearSelection();
        }
        lmsrSharesField.clear();
        orderSharesField.clear();
        orderPriceField.clear();
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    // ----- Actions (all asynchronous: the UI thread never waits for the server) -----

    /** Uploads an events XML file. Its events are added to the system and this user becomes their market maker. */
    @FXML
    private void onUploadFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a Guess Market XML file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File file = chooser.showOpenDialog(uploadButton.getScene().getWindow());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".xml")) {
            UiUtils.showError("The file '" + file.getName() + "' is not an XML file.");
            return;
        }
        setUploading(true);
        CompletableFuture<String> upload = CompletableFuture
                .supplyAsync(() -> readFile(file))
                .thenCompose(content -> ServerApi.uploadFile(Endpoints.UPLOAD_FILE, file.getName(), content));

        UiUtils.whenDone(upload, message -> {
            setUploading(false);
            filePathLabel.setText(file.getAbsolutePath());
            refresher.refreshNow();
            UiUtils.showInfo(message);
        }, error -> {
            setUploading(false);
            UiUtils.showError("The file was not loaded.\n" + error);
        });
    }

    private static byte[] readFile(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the file: " + e.getMessage(), e);
        }
    }

    private void setUploading(boolean uploading) {
        uploadButton.setDisable(uploading);
        progressBar.setVisible(uploading);
        progressBar.setProgress(uploading ? ProgressBar.INDETERMINATE_PROGRESS : 0);
    }

    @FXML
    private void onDeposit() {
        Double amount = parsePositiveDouble(depositField.getText(), "The deposit amount");
        if (amount != null) {
            runAction(ServerApi.post(Endpoints.DEPOSIT, Map.of("amount", String.valueOf(amount))), depositField::clear);
        }
    }

    @FXML
    private void onActivate() {
        runAction(ServerApi.post(Endpoints.ACTIVATE_EVENT, Map.of("event", selectedEventName)), () -> { });
    }

    @FXML
    private void onCloseEvent() {
        String winner = winnerCombo.getValue();
        if (winner == null) {
            UiUtils.showError("Please choose the winning option.");
            return;
        }
        runAction(ServerApi.post(Endpoints.CLOSE_EVENT, Map.of("event", selectedEventName, "winner", winner)), () -> { });
    }

    @FXML
    private void onBuyShares() {
        String option = lmsrOptionCombo.getValue();
        Integer shares = parsePositiveInt(lmsrSharesField.getText(), "The number of shares");
        if (option == null) {
            UiUtils.showError("Please choose an option.");
        } else if (shares != null) {
            runAction(ServerApi.post(Endpoints.BUY_SHARES,
                    Map.of("event", selectedEventName, "option", option, "shares", String.valueOf(shares))),
                    lmsrSharesField::clear);
        }
    }

    @FXML
    private void onPlaceOrder() {
        String option = orderOptionCombo.getValue();
        if (option == null) {
            UiUtils.showError("Please choose an option.");
            return;
        }
        Integer shares = parsePositiveInt(orderSharesField.getText(), "The number of shares");
        Double price = shares == null ? null : parsePositiveDouble(orderPriceField.getText(), "The price per share");
        if (shares != null && price != null) {
            String side = orderSideCombo.getValue().toLowerCase();
            runAction(ServerApi.post(Endpoints.PLACE_ORDER, Map.of("event", selectedEventName, "option", option,
                            "side", side, "shares", String.valueOf(shares), "price", String.valueOf(price))),
                    () -> {
                        orderSharesField.clear();
                        orderPriceField.clear();
                    });
        }
    }

    private void runAction(CompletableFuture<String> request, Runnable afterSuccess) {
        UiUtils.whenDone(request, () -> {
            afterSuccess.run();
            refresher.refreshNow();
        });
    }

    private static Integer parsePositiveInt(String text, String what) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // handled below
        }
        UiUtils.showError(what + " must be a positive whole number.");
        return null;
    }

    private static Double parsePositiveDouble(String text, String what) {
        try {
            double value = Double.parseDouble(text.trim());
            if (value > 0 && !Double.isInfinite(value)) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // handled below
        }
        UiUtils.showError(what + " must be a positive number.");
        return null;
    }
}
