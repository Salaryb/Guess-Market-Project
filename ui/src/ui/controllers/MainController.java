package ui.controllers;

import dto.*;
import engine.GuessMarketEngineImpl;
import engine.IGuessMarketEngine;
import exception.GuessMarketException;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.File;
import java.util.List;

public class MainController {

    private IGuessMarketEngine engine;

    @FXML private Button loadFileBtn;
    @FXML private Label filePathLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    @FXML private TableView<EventDTO> eventsTable;

    @FXML private ToggleGroup typeGroup;
    @FXML private ToggleGroup statusGroup;
    @FXML private ToggleGroup commissionGroup;

    @FXML private Label lblEventCommission;

    private ObservableList<EventDTO> masterEventList = FXCollections.observableArrayList();

    @FXML private Label lblEventId;
    @FXML private Label lblEventStatus;
    @FXML private Label lblEventType;

    @FXML private StackPane eventSpecificDataPane;
    @FXML private VBox lmsrDataPane;
    @FXML private VBox orderBookDataPane;

    // --- Order Book ---
    @FXML private Label lblLast;
    @FXML private Label lblBid;
    @FXML private Label lblAsk;
    @FXML private Label lblMid;
    @FXML private Label lblSpread;
    @FXML private TableView<OrderDTO> buyOrdersTable;
    @FXML private TableView<OrderDTO> sellOrdersTable;

    // --- LMSR ---
    @FXML private Label lblLmsrAccount;
    @FXML private Label lblLmsrCommission;
    @FXML private Label lblLmsrWinner;
    @FXML private TableView<OptionDTO> lmsrStatusTable;
    @FXML private TableView<TradeDTO> lmsrHistoryTable;

    @FXML private TableView<ParticipantDTO> participantsTable;

    // ==========================================
    // USERS TAB: Left Side (System Users)
    // ==========================================
    // Fixed: Changed <?> to <UserDisplayItem> helper class
    @FXML private TableView<UserDisplayItem> usersTable;

    // ==========================================
    // USERS TAB: Right Side (Top - User Info & Events)
    // ==========================================
    @FXML private Label lblUserName;
    @FXML private Label lblUserBalance;
    @FXML private Label lblUserStatus;
    @FXML private TableView<UserEventSummaryDTO> userEventsTable;

    // ==========================================
    // USERS TAB: Right Side (Middle - MM Actions)
    // ==========================================
    @FXML private HBox mmActionsBox;
    @FXML private Button btnStartEvent;
    @FXML private ComboBox<String> cmbWinnerSelection;
    @FXML private Button btnCloseEvent;

    // ==========================================
    // USERS TAB: Right Side (Bottom - Event Details)
    // ==========================================
    @FXML private StackPane userEventDetailsStackPane;

    // --- LMSR Pane & Trading ---
    @FXML private VBox userLmsrPane;
    @FXML private Label lblUserLmsrCommission;
    @FXML private Label lblUserLmsrClosedInfo;
    @FXML private TableView<?> userLmsrHistoryTable;
    @FXML private HBox lmsrTradeBox;
    @FXML private ComboBox<String> cmbLmsrOption;
    @FXML private TextField txtLmsrQuantity;
    @FXML private Button btnLmsrBuy;
    @FXML private Button btnLmsrSell;

    // --- Order Book Pane & Trading ---
    @FXML private VBox userObPane;
    @FXML private Label lblUserObCommission;
    @FXML private Label lblUserObClosedInfo;
    @FXML private TableView<?> userObHoldingsTable;
    @FXML private HBox obTradeBox;
    @FXML private ComboBox<String> cmbObAction;
    @FXML private ComboBox<String> cmbObOption;
    @FXML private TextField txtObQuantity;
    @FXML private TextField txtObPrice;
    @FXML private Button btnObSubmit;

    @FXML
    public void initialize() {
        this.engine = new GuessMarketEngineImpl();

        if (progressBar != null) this.progressBar.setVisible(false);
        if (statusLabel != null) this.statusLabel.setText("Please load an XML file to start.");

        // Hide dynamic panes
        hideAllDynamicPanes();
        if (userLmsrPane != null) userLmsrPane.setVisible(false);
        if (userObPane != null) userObPane.setVisible(false);
        if (mmActionsBox != null) mmActionsBox.setVisible(false);

        // Set up the table columns
        setupEventsTable();
        setupEventSpecificTables();
        setupUsersTable();
        setupUserTabActions();
        setupOrderBookTrading();
        setupLmsrTrading();
        setupParticipantsTable();

        // Listen for clicks on the Users table
        usersTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleUserSelection(newValue);
            }
        });

        // Listen for clicks on the Events table
        eventsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleEventSelection(newValue);
            }
        });
    }

    private void handleEventSelection(EventDTO selectedEvent) {
        if (selectedEvent == null) return;

        // 1. Update Top Labels
        lblEventId.setText(String.valueOf(selectedEvent.id()));
        lblEventType.setText(selectedEvent.type());
        lblEventStatus.setText(selectedEvent.status());
        lblEventCommission.setText(String.format("%.2f%% (%s)", selectedEvent.commission(), selectedEvent.commissionType()));

        // 2. Fetch and Display Dynamic Data
        int currentId = selectedEvent.id();

        if ("LMSR".equalsIgnoreCase(selectedEvent.type())) {
            showLmsrView();

            // Update LMSR Labels
            lblLmsrCommission.setText(String.format("%.2f", engine.getEventTotalCommission(currentId)));
            lblLmsrWinner.setText(engine.getEventWinningOption(currentId));
            lblLmsrAccount.setText(String.format("%.2f", engine.getEventAccountBalance(currentId)));

            // Populate LMSR Tables
            lmsrHistoryTable.setItems(FXCollections.observableArrayList(engine.getLmsrTradeHistory(currentId)));
            lmsrStatusTable.setItems(FXCollections.observableArrayList(engine.getLmsrOptionStatus(currentId)));

        } else {
            showOrderBookView();

            // Populate Order Book Tables
            buyOrdersTable.setItems(FXCollections.observableArrayList(engine.getBuyOrders(currentId)));
            sellOrdersTable.setItems(FXCollections.observableArrayList(engine.getSellOrders(currentId)));

            List<String> options = engine.getEventOptions(currentId);
            if (!options.isEmpty()) {
                updateOrderBookStats(currentId, options.get(0));
            }
        }

        participantsTable.setItems(FXCollections.observableArrayList(engine.getEventParticipants(currentId)));

        System.out.println("Loaded details for Event: " + selectedEvent.id());
    }

    @FXML
    public void handleLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML Data File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        Stage stage = (Stage) loadFileBtn.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            loadXmlFileWithTask(selectedFile.getAbsolutePath());
            filePathLabel.setText("Currently Loaded File path: " + selectedFile.getAbsolutePath());
        }
    }

    private void loadXmlFileWithTask(String filePath) {
        progressBar.setVisible(true);
        loadFileBtn.setDisable(true);

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. Simulate gradual loading to fill the progress bar smoothly
                int totalSteps = 15;
                for (int i = 1; i <= totalSteps; i++) {
                    Thread.sleep(100); // 15 steps * 100ms = 1.5 seconds total
                    updateProgress(i, totalSteps); // Updates the bar!
                    updateMessage("Loading file... " + (int)((i / (double)totalSteps) * 100) + "%"); // Updates the text!
                }

                // 2. Load the actual XML data
                engine.loadDataFromXml(filePath);
                return null;
            }
        };

        // BIND the UI elements to listen to the Task's progress and messages
        progressBar.progressProperty().bind(loadTask.progressProperty());
        statusLabel.textProperty().bind(loadTask.messageProperty());

        loadTask.setOnSucceeded(e -> {
            // UNBIND them so we can manually change the text again
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();

            progressBar.setVisible(false);
            statusLabel.setText("File loaded successfully.");
            loadFileBtn.setDisable(false);
            refreshAllViews();
        });

        loadTask.setOnFailed(e -> {
            // UNBIND them here too
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();

            progressBar.setVisible(false);
            statusLabel.setText("Failed to load file.");
            loadFileBtn.setDisable(false);

            Throwable exception = loadTask.getException();
            String errorMessage = exception instanceof GuessMarketException ?
                    exception.getMessage() : "Unknown error occurred while loading the file.";
            showErrorAlert(errorMessage);
        });

        // Start the background thread
        new Thread(loadTask).start();
    }

    private void refreshAllViews() {
        try {
            // ==========================================
            // 1. SAVE CURRENT SELECTIONS (The "Memory")
            // ==========================================
            EventDTO selectedMainEvent = eventsTable.getSelectionModel().getSelectedItem();
            Integer savedMainEventId = (selectedMainEvent != null) ? selectedMainEvent.id() : null;

            UserDisplayItem selectedUser = usersTable.getSelectionModel().getSelectedItem();
            String savedUserName = (selectedUser != null) ? selectedUser.getName() : null;

            UserEventSummaryDTO selectedUserEvent = userEventsTable.getSelectionModel().getSelectedItem();
            Integer savedUserEventId = (selectedUserEvent != null) ? selectedUserEvent.eventId() : null;

            // ==========================================
            // 2. REFRESH ALL DATA (Your original code)
            // ==========================================
            List<EventDTO> eventsList = engine.getAllEvents();
            masterEventList.setAll(eventsList);

            FilteredList<EventDTO> filteredEvents = new FilteredList<>(masterEventList, b -> true);
            eventsTable.setItems(filteredEvents);
            setupFilterListeners(filteredEvents);

            List<String> usernames = engine.getAllUsernames();
            ObservableList<UserDisplayItem> userItems = FXCollections.observableArrayList();

            for (String username : usernames) {
                double balance = engine.getUserBalance(username);
                userItems.add(new UserDisplayItem(username, balance));
            }
            usersTable.setItems(userItems);

            // ==========================================
            // 3. RESTORE SELECTIONS (The "Magic")
            // ==========================================

            // Restore Main Events Table Selection
            if (savedMainEventId != null) {
                for (EventDTO e : eventsTable.getItems()) {
                    if (e.id() == savedMainEventId) {
                        eventsTable.getSelectionModel().select(e);
                        break;
                    }
                }
            }

            // Restore Users Table Selection
            if (savedUserName != null) {
                for (UserDisplayItem u : usersTable.getItems()) {
                    if (u.getName().equals(savedUserName)) {
                        usersTable.getSelectionModel().select(u);

                        // Because selecting the user triggers a listener that populates
                        // the userEventsTable, we can now safely restore the event selection!
                        if (savedUserEventId != null) {
                            for (UserEventSummaryDTO ue : userEventsTable.getItems()) {
                                if (ue.eventId() == savedUserEventId) {
                                    userEventsTable.getSelectionModel().select(ue);
                                    handleUserEventSelection(ue);
                                    break;
                                }
                            }
                        }
                        break; // Stop looping once we found our user
                    }
                }
            }

            System.out.println("Data successfully loaded and selections restored!");
        } catch (Exception e) {
            showErrorAlert("Failed to populate tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Validation Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void hideAllDynamicPanes() {
        if (lmsrDataPane != null) lmsrDataPane.setVisible(false);
        if (orderBookDataPane != null) orderBookDataPane.setVisible(false);
    }

    public void showLmsrView() {
        hideAllDynamicPanes();
        if (lmsrDataPane != null) lmsrDataPane.setVisible(true);
    }

    public void showOrderBookView() {
        hideAllDynamicPanes();
        if (orderBookDataPane != null) orderBookDataPane.setVisible(true);
    }

    // ==========================================
    // TABLE SETUP METHODS
    // ==========================================

    private void setupEventsTable() {
        eventsTable.getColumns().clear();

        // 1. Event ID Column (Note: Your id is an int, so we use Integer here)
        TableColumn<EventDTO, Integer> idCol = new TableColumn<>("Event ID");
        idCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().id()));

        // 2. Type Column
        TableColumn<EventDTO, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().type()));

        // 3. Status Column
        TableColumn<EventDTO, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().status()));

        eventsTable.getColumns().addAll(idCol, typeCol, statusCol);
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupUsersTable() {
        usersTable.getColumns().clear();

        TableColumn<UserDisplayItem, String> nameCol = new TableColumn<>("User Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<UserDisplayItem, Double> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));

        usersTable.getColumns().addAll(nameCol, balanceCol);
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // ==========================================
    // UI HELPER CLASSES
    // ==========================================

    /**
     * A helper class strictly for the UI to bind the username strings and their
     * corresponding balances into a single object for the TableView.
     */
    public static class UserDisplayItem {
        private final String name;
        private final double balance;

        public UserDisplayItem(String name, double balance) {
            this.name = name;
            this.balance = balance;
        }

        public String getName() { return name; }
        public double getBalance() { return balance; }
    }

    private void handleUserSelection(UserDisplayItem selectedUser) {
        // 1. Update the basic info labels on the right side
        lblUserName.setText(selectedUser.getName());
        lblUserBalance.setText(String.format("%.2f", selectedUser.getBalance()));

        // 2. Ask the engine if this user is blocked
        boolean isBlocked = engine.isUserBlocked(selectedUser.getName());

        if (isBlocked) {
            lblUserStatus.setText("BLOCKED");
            lblUserStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); // Make it red!
        } else {
            lblUserStatus.setText("Active");
            lblUserStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;"); // Make it green!
        }

        System.out.println("User selected in UI: " + selectedUser.getName());

        List<UserEventSummaryDTO> userEvents = engine.getUserActiveEvents(selectedUser.getName());
        userEventsTable.setItems(FXCollections.observableArrayList(userEvents));
        userLmsrPane.setVisible(false);
        userObPane.setVisible(false);
        mmActionsBox.setVisible(false);
    }

    private void setupFilterListeners(FilteredList<EventDTO> filteredEvents) {
        // Trigger filter check whenever any toggle group changes, but check if they exist first!
        if (typeGroup != null) {
            typeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> applyEventFilters(filteredEvents));
        }

        if (statusGroup != null) {
            statusGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> applyEventFilters(filteredEvents));
        }

        if (commissionGroup != null) {
            commissionGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> applyEventFilters(filteredEvents));
        } else {
            System.out.println("Warning: commissionGroup is null. Please check Scene Builder fx:id.");
        }
    }

    private void applyEventFilters(FilteredList<EventDTO> filteredEvents) {
        filteredEvents.setPredicate(event -> {
            boolean matchesType = true;
            boolean matchesStatus = true;

            // Type Filter
            ToggleButton selectedType = (ToggleButton) typeGroup.getSelectedToggle();
            if (selectedType != null && !selectedType.getText().equalsIgnoreCase("All")) {
                matchesType = event.type().equalsIgnoreCase(selectedType.getText());
            }

            // Status Filter
            ToggleButton selectedStatus = (ToggleButton) statusGroup.getSelectedToggle();
            if (selectedStatus != null && !selectedStatus.getText().equalsIgnoreCase("All")) {

                String filterText = selectedStatus.getText();
                // Translate the UI button text to match your engine's DTO text
                if (filterText.equalsIgnoreCase("Not Started")) {
                    filterText = "Pending";
                }

                matchesStatus = event.status().equalsIgnoreCase(filterText);
            }

            return matchesType && matchesStatus;
        });
    }

    private void setupEventSpecificTables() {
        // ==========================================
        // 1. ORDER BOOK: BUY ORDERS TABLE (OrderDTO)
        // ==========================================
        buyOrdersTable.getColumns().clear();
        TableColumn<OrderDTO, String> buyUserCol = new TableColumn<>("User");
        buyUserCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().userName()));
        TableColumn<OrderDTO, Integer> buySharesCol = new TableColumn<>("Shares");
        buySharesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().shares()));
        TableColumn<OrderDTO, Double> buyPriceCol = new TableColumn<>("Price");
        buyPriceCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().price()));
        buyOrdersTable.getColumns().addAll(buyUserCol, buySharesCol, buyPriceCol);
        buyOrdersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ==========================================
        // 2. ORDER BOOK: SELL ORDERS TABLE (OrderDTO)
        // ==========================================
        sellOrdersTable.getColumns().clear();
        TableColumn<OrderDTO, String> sellUserCol = new TableColumn<>("User");
        sellUserCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().userName()));
        TableColumn<OrderDTO, Integer> sellSharesCol = new TableColumn<>("Shares");
        sellSharesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().shares()));
        TableColumn<OrderDTO, Double> sellPriceCol = new TableColumn<>("Price");
        sellPriceCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().price()));
        sellOrdersTable.getColumns().addAll(sellUserCol, sellSharesCol, sellPriceCol);
        sellOrdersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ==========================================
        // 3. LMSR: STATUS TABLE (OptionDTO)
        // ==========================================
        lmsrStatusTable.getColumns().clear();
        TableColumn<OptionDTO, String> optNameCol = new TableColumn<>("Option");
        optNameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().name()));
        TableColumn<OptionDTO, Integer> optSharesCol = new TableColumn<>("Shares Bought");
        optSharesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().sharesBought()));
        lmsrStatusTable.getColumns().addAll(optNameCol, optSharesCol);
        lmsrStatusTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ==========================================
        // 4. LMSR: HISTORY TABLE (TradeDTO)
        // ==========================================
        lmsrHistoryTable.getColumns().clear();
        TableColumn<TradeDTO, String> trdOptionCol = new TableColumn<>("Option");
        trdOptionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().optionName()));
        TableColumn<TradeDTO, Integer> trdSharesCol = new TableColumn<>("Shares");
        trdSharesCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().shares()));
        TableColumn<TradeDTO, String> trdPriceCol = new TableColumn<>("Price Paid");
        trdPriceCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f", cell.getValue().pricePaid())));
        lmsrHistoryTable.getColumns().addAll(trdOptionCol, trdSharesCol, trdPriceCol);
        lmsrHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void handleUserEventSelection(UserEventSummaryDTO selectedEvent) {
        if (selectedEvent == null) {
            mmActionsBox.setVisible(false);
            userLmsrPane.setVisible(false);
            userObPane.setVisible(false);
            return;
        }

        mmActionsBox.setVisible(true);

        EventDTO fullEvent = engine.getAllEvents().stream()
                .filter(e -> e.id() == selectedEvent.eventId())
                .findFirst().orElse(null);

        if (fullEvent != null) {
            List<String> options = engine.getEventOptions(selectedEvent.eventId());
            cmbObOption.setItems(FXCollections.observableArrayList(options));
            cmbLmsrOption.setItems(FXCollections.observableArrayList(options));
            cmbWinnerSelection.setItems(FXCollections.observableArrayList(options));

            if ("LMSR".equalsIgnoreCase(fullEvent.type())) {
                userLmsrPane.setVisible(true);
                userObPane.setVisible(false);
            } else {
                userObPane.setVisible(true);
                userLmsrPane.setVisible(false);
            }
        }
    }

    private void setupUserTabActions() {
        userEventsTable.getColumns().clear();

        TableColumn<UserEventSummaryDTO, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().eventId()));

        TableColumn<UserEventSummaryDTO, String> nameCol = new TableColumn<>("Event Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().eventName()));

        userEventsTable.getColumns().addAll(idCol, nameCol);
        userEventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        userEventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selectedEvent) -> {
            handleUserEventSelection(selectedEvent);
        });

        btnStartEvent.setOnAction(e -> {
            UserEventSummaryDTO selectedEvent = userEventsTable.getSelectionModel().getSelectedItem();
            UserDisplayItem selectedUser = usersTable.getSelectionModel().getSelectedItem();

            if (selectedEvent != null && selectedUser != null) {
                try {
                    // This calls your engine to start the event!
                    engine.activateEvent(selectedEvent.eventId(), selectedUser.getName());

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "Event is now Active! Users can begin trading.");
                    successAlert.showAndWait();

                    refreshAllViews(); // Updates the main tables to show "Active"
                } catch (Exception ex) {
                    // Will throw an error if the user is NOT the Market Maker or lacks funds
                    showErrorAlert(ex.getMessage());
                }
            }
        });

        btnCloseEvent.setOnAction(e -> {
            UserEventSummaryDTO selectedEvent = userEventsTable.getSelectionModel().getSelectedItem();
            UserDisplayItem selectedUser = usersTable.getSelectionModel().getSelectedItem();
            String winningOption = cmbWinnerSelection.getValue();

            if (selectedEvent == null || selectedUser == null || winningOption == null) {
                showErrorAlert("Please select a user, an event, and the winning option.");
                return;
            }

            try {
                // Execute the closing payout!
                engine.closeEvent(selectedEvent.eventId(), selectedUser.getName(), winningOption);

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "Event closed! Winnings have been distributed.");
                successAlert.showAndWait();

                refreshAllViews(); // Updates tables to show "Ended" and new user balances
            } catch (Exception ex) {
                showErrorAlert(ex.getMessage());
            }
        });
    }

    private void setupOrderBookTrading() {
        // 1. Set the static Buy/Sell options
        cmbObAction.setItems(FXCollections.observableArrayList("Buy", "Sell"));

        // 2. Attach the click listener to the Submit Button
        btnObSubmit.setOnAction(e -> {
            UserDisplayItem selectedUser = usersTable.getSelectionModel().getSelectedItem();
            UserEventSummaryDTO selectedEvent = userEventsTable.getSelectionModel().getSelectedItem();

            if (selectedUser == null || selectedEvent == null) {
                showErrorAlert("Please select a user and an event first.");
                return;
            }

            try {
                // Grab the values from the UI
                String action = cmbObAction.getValue();
                String option = cmbObOption.getValue();

                if (action == null || option == null) {
                    showErrorAlert("Please select an action (Buy/Sell) and an option.");
                    return;
                }

                int quantity = Integer.parseInt(txtObQuantity.getText());
                double price = Double.parseDouble(txtObPrice.getText());
                boolean isBuy = action.equals("Buy");

                // Execute the trade in the engine!
                engine.placeOrderBookOrder(selectedEvent.eventId(), selectedUser.getName(), option, quantity, price, isBuy);

                // Show success and clear the text fields
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "Order submitted successfully!");
                successAlert.showAndWait();

                txtObQuantity.clear();
                txtObPrice.clear();

                // Refresh the tables to instantly show the new balance and orders
                refreshAllViews();

            } catch (NumberFormatException ex) {
                showErrorAlert("Quantity must be a whole number, and Price must be a valid decimal/number.");
            } catch (Exception ex) {
                showErrorAlert(ex.getMessage()); // Catches blocked users, inactive events, insufficient funds, etc.
            }
        });
    }

    private void setupLmsrTrading() {
        btnLmsrBuy.setOnAction(e -> {
            UserDisplayItem selectedUser = usersTable.getSelectionModel().getSelectedItem();
            UserEventSummaryDTO selectedEvent = userEventsTable.getSelectionModel().getSelectedItem();

            if (selectedUser == null || selectedEvent == null) {
                showErrorAlert("Please select a user and an event first.");
                return;
            }

            try {
                String option = cmbLmsrOption.getValue();
                if (option == null) {
                    showErrorAlert("Please select an option to buy.");
                    return;
                }

                int quantity = Integer.parseInt(txtLmsrQuantity.getText());

                // Execute the trade!
                engine.placeLmsrOrder(selectedEvent.eventId(), selectedUser.getName(), option, quantity);

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "LMSR Order submitted successfully!");
                successAlert.showAndWait();

                txtLmsrQuantity.clear();
                refreshAllViews();

            } catch (NumberFormatException ex) {
                showErrorAlert("Quantity must be a whole number.");
            } catch (Exception ex) {
                showErrorAlert(ex.getMessage());
            }
        });
    }

    private void setupParticipantsTable() {
        participantsTable.getColumns().clear();

        TableColumn<ParticipantDTO, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));

        TableColumn<ParticipantDTO, Integer> qtyCol = new TableColumn<>("Holdings Quantity");
        qtyCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().quantity()));

        TableColumn<ParticipantDTO, String> valCol = new TableColumn<>("Holdings Value");
        // Forces the required 2-decimal formatting string!
        valCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().value())));

        participantsTable.getColumns().addAll(nameCol, qtyCol, valCol);
        participantsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void updateOrderBookStats(int eventId, String optionName) {
        // 1. Filter orders for this specific option
        List<OrderDTO> buys = engine.getBuyOrders(eventId).stream()
                .filter(o -> o.option().equals(optionName)).toList();
        List<OrderDTO> sells = engine.getSellOrders(eventId).stream()
                .filter(o -> o.option().equals(optionName)).toList();

        // 2. Find BID (Max Buy) and ASK (Min Sell)
        double bid = buys.stream().mapToDouble(OrderDTO::price).max().orElse(0.0);
        double ask = sells.stream().mapToDouble(OrderDTO::price).min().orElse(0.0);
        double last = engine.getLastTradePrice(eventId, optionName);

        // 3. Update the UI Labels (Formatting to 2 decimal places!)
        lblLast.setText(last > 0 ? String.format("%.2f", last) : "N/A");
        lblBid.setText(bid > 0 ? String.format("%.2f", bid) : "N/A");
        lblAsk.setText(ask > 0 ? String.format("%.2f", ask) : "N/A");

        // 4. Calculate MID and SPREAD if both Bid and Ask exist
        if (bid > 0 && ask > 0) {
            lblMid.setText(String.format("%.2f", (bid + ask) / 2.0));
            lblSpread.setText(String.format("%.2f", ask - bid));
        } else {
            lblMid.setText("N/A");
            lblSpread.setText("N/A");
        }
    }
}