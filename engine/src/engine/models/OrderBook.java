package engine.models;
import java.util.PriorityQueue;
import java.util.Collections;
import java.util.Map;

public class OrderBook {
    private final PriorityQueue<Order> bids; // Max-Heap לקונים
    private final PriorityQueue<Order> asks; // Min-Heap למוכרים
    private double lastTradePrice = 0.0;

    public OrderBook() {
        this.bids = new PriorityQueue<>();
        this.asks = new PriorityQueue<>(Collections.reverseOrder());
    }

    public void addBid(Order order) { bids.add(order); }
    public void addAsk(Order order) { asks.add(order); }
    public PriorityQueue<Order> getBids() { return bids; }
    public PriorityQueue<Order> getAsks() { return asks; }

    public void matchWithAsks(Order incomingBid, Map<String, User> users, int eventId, String optionName) {
        while (incomingBid.getShares() > 0 && !asks.isEmpty()) {
            Order lowestAsk = asks.peek();
            if (incomingBid.getPrice() >= lowestAsk.getPrice()) {
                int sharesToTrade = Math.min(incomingBid.getShares(), lowestAsk.getShares());
                double tradePrice = lowestAsk.getPrice(); // Buyer pays seller's price

                incomingBid.reduceShares(sharesToTrade);
                lowestAsk.reduceShares(sharesToTrade);
                this.lastTradePrice = tradePrice;

                // 1. BUYER PAYS AND GETS SHARES
                users.get(incomingBid.getUserName()).updateBalance(-(sharesToTrade * tradePrice));
                users.get(incomingBid.getUserName()).addHoldings(eventId, optionName, sharesToTrade);
                users.get(incomingBid.getUserName()).addEventCashFlow(eventId, -(sharesToTrade * tradePrice));

                // 2. SELLER GETS MONEY AND LOSES SHARES
                users.get(lowestAsk.getUserName()).updateBalance(sharesToTrade * tradePrice);
                users.get(lowestAsk.getUserName()).addHoldings(eventId, optionName, -sharesToTrade);
                users.get(lowestAsk.getUserName()).addEventCashFlow(eventId, sharesToTrade * tradePrice);

                if (lowestAsk.getShares() == 0) asks.poll();
            } else {
                break;
            }
        }
    }

    public double getLastTradePrice() { return lastTradePrice; }
    public double getHighestBid() { return bids.isEmpty() ? 0.0 : bids.peek().getPrice(); }
    public double getLowestAsk() { return asks.isEmpty() ? 0.0 : asks.peek().getPrice(); }
    public double getMidPrice() {
        if (bids.isEmpty() || asks.isEmpty()) return 0.0;
        return (getHighestBid() + getLowestAsk()) / 2.0;
    }
    public double getSpread() {
        if (bids.isEmpty() || asks.isEmpty()) return 0.0;
        return getLowestAsk() - getHighestBid();
    }
}