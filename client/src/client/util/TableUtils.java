package client.util;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Helpers for building table columns and for refreshing tables without flickering. */
public final class TableUtils {

    private TableUtils() {
    }

    public static <S> TableColumn<S, String> textColumn(String title, Function<S, String> getter) {
        TableColumn<S, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        return column;
    }

    public static <S> TableColumn<S, Integer> intColumn(String title, Function<S, Integer> getter) {
        TableColumn<S, Integer> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(getter.apply(cell.getValue())));
        return column;
    }

    /** A decimal column shown with 2 digits after the point (sorting still works numerically). */
    public static <S> TableColumn<S, Double> moneyColumn(String title, Function<S, Double> getter) {
        TableColumn<S, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(getter.apply(cell.getValue())));
        column.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : Format.money(value));
            }
        });
        return column;
    }

    @SafeVarargs
    public static <S> void setColumns(TableView<S> table, TableColumn<S, ?>... columns) {
        table.getColumns().setAll(columns);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    /**
     * Replaces the items only if the data really changed (DTOs are records, so equals compares content).
     * The selected row is kept by its key, so the periodic refresh does not lose the user's selection.
     * @return true if the items were replaced
     */
    public static <S> boolean updateItems(TableView<S> table, ObservableList<S> items, List<S> newItems, Function<S, String> key) {
        if (items.equals(newItems)) {
            return false;
        }
        S selected = table.getSelectionModel().getSelectedItem();
        String selectedKey = selected != null ? key.apply(selected) : null;
        items.setAll(newItems);
        if (selectedKey != null) {
            table.getItems().stream()
                    .filter(item -> Objects.equals(key.apply(item), selectedKey))
                    .findFirst()
                    .ifPresent(item -> table.getSelectionModel().select(item));
        }
        return true;
    }

    public static <S> boolean updateItems(TableView<S> table, List<S> newItems) {
        return updateItems(table, table.getItems(), newItems, Object::toString);
    }
}
