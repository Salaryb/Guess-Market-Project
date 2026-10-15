package client.controllers;

import client.refresh.DataRefresher;
import client.refresh.Snapshot;
import client.util.Format;
import client.util.TableUtils;
import dto.EventDTO;
import dto.EventDetailsDTO;
import dto.OptionStatusDTO;
import dto.OrderBookDTO;
import dto.OrderDTO;
import dto.ParticipantDTO;
import dto.TradeDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The Events tab: all events in the system with filters, and the full details of the selected event
 * (LMSR state and history, or the order book of every option, plus the participants).
 */
public class EventsController {
    private static final String ALL = "All";
    private static final double BOOK_TABLE_HEIGHT = 140;

    @FXML private ToggleGroup typeGroup;
    @FXML private ToggleGroup statusGroup;
    @FXML private ToggleGroup commissionGroup;
    @FXML private TableView<EventDTO> eventsTable;

    @FXML private Label placeholderLabel;
    @FXML private VBox detailsBox;
    @FXML private Label eventNameLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label typeLabel;
    @FXML private Label statusLabel;
    @FXML private Label commissionLabel;
    @FXML private Label marketMakerLabel;
    @FXML private Label accountLabel;
    @FXML private Label totalCommissionLabel;
    @FXML private Label winnerLabel;

    @FXML private VBox lmsrBox;
    @FXML private TableView<OptionStatusDTO> lmsrOptionsTable;
    @FXML private TableView<TradeDTO> lmsrTradesTable;
    @FXML private VBox orderBookBox;
    @FXML private HBox orderBooksContainer;
    @FXML private TableView<ParticipantDTO> participantsTable;

    private final ObservableList<EventDTO> allEvents = FXCollections.observableArrayList();
    private final FilteredList<EventDTO> filteredEvents = new FilteredList<>(allEvents, e -> true);
    private DataRefresher refresher;
    private String selectedEventName;
    private EventDetailsDTO shownDetails;
    private List<OrderBookDTO> shownOrderBooks;
    private boolean updatingTable = false;

    public void init(DataRefresher refresher) {
        this.refresher = refresher;
        setupEventsTable();
        setupDetailsTables();
        setupFilters();
        showDetails(null);
    }

    private void setupEventsTable() {
        TableUtils.setColumns(eventsTable,
                TableUtils.textColumn("Name", EventDTO::name),
                TableUtils.textColumn("Type", EventDTO::type),
                TableUtils.textColumn("Status", EventDTO::status),
                TableUtils.textColumn("Commission", e -> e.commission() + "% (" + e.commissionType() + ")"),
                TableUtils.moneyColumn("Event Account", EventDTO::accountBalance),
                TableUtils.textColumn("Market Maker", EventDTO::marketMaker));
        SortedList<EventDTO> sorted = new SortedList<>(filteredEvents);
        sorted.comparatorProperty().bind(eventsTable.comparatorProperty());
        eventsTable.setItems(sorted);
        eventsTable.setPlaceholder(new Label("No events yet. Upload an XML file to add events."));

        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingTable) {
                onEventSelected(newValue);
            }
        });
    }

    private void setupDetailsTables() {
        TableUtils.setColumns(lmsrOptionsTable,
                TableUtils.textColumn("Option", OptionStatusDTO::option),
                TableUtils.moneyColumn("Current Value", OptionStatusDTO::price),
                TableUtils.intColumn("Shares Bought", OptionStatusDTO::sharesBought));
        TableUtils.setColumns(lmsrTradesTable,
                TableUtils.textColumn("User", TradeDTO::userName),
                TableUtils.textColumn("Option", TradeDTO::option),
                TableUtils.intColumn("Shares", TradeDTO::shares),
                TableUtils.moneyColumn("Price Paid", TradeDTO::pricePaid),
                TableUtils.moneyColumn("Commission", TradeDTO::commission));
        TableUtils.setColumns(participantsTable,
                TableUtils.textColumn("Name", ParticipantDTO::name),
                holdingsColumn(),
                TableUtils.intColumn("Open Orders", ParticipantDTO::openOrders),
                TableUtils.moneyColumn("Total Value", ParticipantDTO::totalValue));
    }

    private static TableColumn<ParticipantDTO, String> holdingsColumn() {
        TableColumn<ParticipantDTO, String> column =
                TableUtils.textColumn("Holdings (shares / value)", EventsController::describeHoldings);
        column.setPrefWidth(320);
        return column;
    }

    private static String describeHoldings(ParticipantDTO participant) {
        return participant.holdings().stream()
                .map(h -> h.option() + ": " + h.shares() + " (" + Format.money(h.value()) + ")")
                .collect(Collectors.joining("  |  "));
    }

    // ----- Filters -----

    private void setupFilters() {
        for (ToggleGroup group : List.of(typeGroup, statusGroup, commissionGroup)) {
            group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                if (newToggle == null) {
                    oldToggle.setSelected(true); // one button of each filter is always selected
                } else {
                    filteredEvents.setPredicate(this::matchesFilters);
                }
            });
        }
    }

    private boolean matchesFilters(EventDTO event) {
        return matches(typeGroup, event.type()) && matches(statusGroup, event.status())
                && matches(commissionGroup, event.commissionType());
    }

    private static boolean matches(ToggleGroup group, String value) {
        ToggleButton selected = (ToggleButton) group.getSelectedToggle();
        return selected == null || selected.getText().equals(ALL) || selected.getText().equalsIgnoreCase(value);
    }

    // ----- Data updates -----

    private void onEventSelected(EventDTO event) {
        String name = event == null ? null : event.name();
        if (name != null && name.equals(selectedEventName)) {
            return;
        }
        selectedEventName = name;
        showDetails(null);
        if (name != null) {
            placeholderLabel.setText("Loading '" + name + "'...");
        }
        refresher.setSelectedEvent(name);
    }

    public void update(Snapshot snapshot) {
        updatingTable = true;
        TableUtils.updateItems(eventsTable, allEvents, snapshot.events(), EventDTO::name);
        updatingTable = false;

        EventDetailsDTO details = snapshot.eventDetails();
        if (details != null && details.event().name().equals(selectedEventName) && !details.equals(shownDetails)) {
            showDetails(details);
        }
    }

    private void showDetails(EventDetailsDTO details) {
        shownDetails = details;
        boolean hasDetails = details != null;
        detailsBox.setVisible(hasDetails);
        detailsBox.setManaged(hasDetails);
        placeholderLabel.setVisible(!hasDetails);
        placeholderLabel.setManaged(!hasDetails);
        placeholderLabel.setText("Select an event to see its details.");
        if (!hasDetails) {
            shownOrderBooks = null;
            return;
        }

        EventDTO event = details.event();
        eventNameLabel.setText(event.name());
        descriptionLabel.setText(event.description());
        typeLabel.setText(event.type());
        statusLabel.setText(event.status());
        commissionLabel.setText(event.commission() + "% (" + event.commissionType() + ")");
        marketMakerLabel.setText(event.marketMaker());
        accountLabel.setText(Format.money(event.accountBalance()));
        totalCommissionLabel.setText(Format.money(details.totalCommission()));
        winnerLabel.setText(details.winningOption() == null ? "-" : details.winningOption());

        boolean isLmsr = "LMSR".equals(event.type());
        setShown(lmsrBox, isLmsr);
        setShown(orderBookBox, !isLmsr);
        if (isLmsr) {
            TableUtils.updateItems(lmsrOptionsTable, details.lmsrOptions());
            TableUtils.updateItems(lmsrTradesTable, details.lmsrTrades());
        } else if (!details.orderBooks().equals(shownOrderBooks)) {
            shownOrderBooks = details.orderBooks();
            orderBooksContainer.getChildren().setAll(details.orderBooks().stream().map(this::createOrderBookView).toList());
        }
        TableUtils.updateItems(participantsTable, participantsTable.getItems(), details.participants(), ParticipantDTO::name);
    }

    /** The order book of a single option: statistics + bids table + asks table (options are shown side by side). */
    private VBox createOrderBookView(OrderBookDTO book) {
        Label title = new Label("Option: " + book.option());
        title.getStyleClass().add("section-title");
        Label stats = new Label(String.format("LAST: %s   BID: %s   ASK: %s%nMID: %s   SPREAD: %s",
                Format.moneyOrNA(book.last()), Format.moneyOrNA(book.bid()), Format.moneyOrNA(book.ask()),
                Format.moneyOrNA(book.mid()), Format.moneyOrNA(book.spread())));

        VBox view = new VBox(6, title, stats, createOrdersBox("Buy orders (bids)", book.bids()),
                createOrdersBox("Sell orders (asks)", book.asks()));
        view.setPadding(new Insets(8));
        view.getStyleClass().add("card");
        view.setMinWidth(0);
        HBox.setHgrow(view, Priority.ALWAYS);
        view.setMaxWidth(Double.MAX_VALUE);
        return view;
    }

    private VBox createOrdersBox(String title, List<OrderDTO> orders) {
        TableView<OrderDTO> table = new TableView<>(FXCollections.observableArrayList(orders));
        TableUtils.setColumns(table,
                TableUtils.textColumn("User", OrderDTO::userName),
                TableUtils.intColumn("Shares", OrderDTO::shares),
                TableUtils.moneyColumn("Price per Share", OrderDTO::price));
        table.setPrefHeight(BOOK_TABLE_HEIGHT);
        table.setPlaceholder(new Label("No orders"));
        return new VBox(4, new Label(title), table);
    }

    private static void setShown(VBox box, boolean shown) {
        box.setVisible(shown);
        box.setManaged(shown);
    }
}
