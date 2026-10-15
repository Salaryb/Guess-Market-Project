package engine.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The order book of a single option.
 * Bids are kept from the highest price to the lowest, asks from the lowest to the highest.
 * Orders with the same price are kept by arrival time (first come, first served).
 */
public class OrderBook {
    private static final Comparator<Order> BIDS_ORDER =
            Comparator.comparingDouble(Order::getPrice).reversed().thenComparingLong(Order::getSequence);
    private static final Comparator<Order> ASKS_ORDER =
            Comparator.comparingDouble(Order::getPrice).thenComparingLong(Order::getSequence);

    private final String option;
    private final List<Order> bids = new ArrayList<>();
    private final List<Order> asks = new ArrayList<>();
    private Double lastTradePrice = null;

    public OrderBook(String option) {
        this.option = option;
    }

    public String getOption() { return option; }
    public List<Order> getBids() { return bids; }
    public List<Order> getAsks() { return asks; }
    public Double getLastTradePrice() { return lastTradePrice; }
    public void setLastTradePrice(double price) { lastTradePrice = price; }

    public void addOrder(Order order) {
        List<Order> side = order.isBuy() ? bids : asks;
        side.add(order);
        side.sort(order.isBuy() ? BIDS_ORDER : ASKS_ORDER);
    }

    public void removeFilledOrders() {
        bids.removeIf(Order::isFilled);
        asks.removeIf(Order::isFilled);
    }

    public void clear() {
        bids.clear();
        asks.clear();
    }

    public Double getHighestBid() { return bids.isEmpty() ? null : bids.get(0).getPrice(); }
    public Double getLowestAsk() { return asks.isEmpty() ? null : asks.get(0).getPrice(); }

    public Double getMidPrice() {
        return (bids.isEmpty() || asks.isEmpty()) ? null : (getHighestBid() + getLowestAsk()) / 2.0;
    }

    public Double getSpread() {
        return (bids.isEmpty() || asks.isEmpty()) ? null : getLowestAsk() - getHighestBid();
    }
}
