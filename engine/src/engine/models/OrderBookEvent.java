package engine.models;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class OrderBookEvent extends Event {
    private final Map<String, OrderBook> orderBooks;
    private final boolean allowMint;
    private final int baseValueD;
    private final int initialInvestment;
    private Map<String, Double> lastPrices = new HashMap<>();

    public OrderBookEvent(int id, String name, String description, double commission, String commissionType,
                          List<String> optionNames, boolean allowMint, int baseValueD, int initialInvestment) {
        super(id, name, description, commission, commissionType, optionNames);
        this.allowMint = allowMint;
        this.baseValueD = baseValueD;
        this.initialInvestment = initialInvestment;
        this.orderBooks = new HashMap<>();
        for (String opt : optionNames) {
            this.orderBooks.put(opt, new OrderBook());
        }
    }

    @Override
    public boolean isOrderBook() { return true; }
    public int getBaseValueD() { return baseValueD; }
    public int getInitialInvestment() { return initialInvestment; }
    public OrderBook getOrderBook(String option) { return orderBooks.get(option); }

    public void handleNewOrder(String optionName, Order newOrder, Map<String, User> users, int eventId) {
        OrderBook targetBook = orderBooks.get(optionName);
        OrderBook oppositeBook = getOppositeBook(optionName);

        if (newOrder.isBuy()) {
            processBidOrder(newOrder, targetBook, oppositeBook, optionName, users, eventId);
        } else {
            targetBook.addAsk(newOrder);
        }
    }

    private void processBidOrder(Order incomingBid, OrderBook targetBook, OrderBook oppositeBook, String optionName, Map<String, User> users, int eventId) {
        // MATCH STANDARD TRADES FIRST (Passes data to the OrderBook)
        targetBook.matchWithAsks(incomingBid, users, eventId, optionName);

        // MINT LOGIC
        if (this.allowMint && incomingBid.getShares() > 0) {
            PriorityQueue<Order> oppositeBids = oppositeBook.getBids();
            while (incomingBid.getShares() > 0 && !oppositeBids.isEmpty()) {
                Order restingOppositeBid = oppositeBids.peek();
                if (incomingBid.getPrice() + restingOppositeBid.getPrice() >= baseValueD) {
                    int sharesToMint = Math.min(incomingBid.getShares(), restingOppositeBid.getShares());
                    incomingBid.reduceShares(sharesToMint);
                    restingOppositeBid.reduceShares(sharesToMint);
                    this.addToAccountBalance(baseValueD * sharesToMint);

                    // 1. CHARGE USERS FOR THE MINT
                    users.get(incomingBid.getUserName()).updateBalance(-(sharesToMint * incomingBid.getPrice()));
                    users.get(restingOppositeBid.getUserName()).updateBalance(-(sharesToMint * restingOppositeBid.getPrice()));

                    users.get(incomingBid.getUserName()).addEventCashFlow(eventId, -(sharesToMint * incomingBid.getPrice()));
                    users.get(restingOppositeBid.getUserName()).addEventCashFlow(eventId, -(sharesToMint * restingOppositeBid.getPrice()));

                    // 2. ADD SHARES TO HOLDINGS
                    users.get(incomingBid.getUserName()).addHoldings(eventId, optionName, sharesToMint);
                    String oppOpt = orderBooks.keySet().stream().filter(k -> !k.equals(optionName)).findFirst().get();
                    users.get(restingOppositeBid.getUserName()).addHoldings(eventId, oppOpt, sharesToMint);

                    // Record Prices for UI
                    this.setLastPrice(optionName, incomingBid.getPrice());
                    this.setLastPrice(oppOpt, restingOppositeBid.getPrice());

                    if (restingOppositeBid.getShares() == 0) oppositeBids.poll();
                } else {
                    break;
                }
            }
        }

        if (incomingBid.getShares() > 0) {
            targetBook.addBid(incomingBid);
        }
    }

    private OrderBook getOppositeBook(String optionName) {
        for (String key : orderBooks.keySet()) {
            if (!key.equals(optionName)) return orderBooks.get(key);
        }
        return null;
    }

    public void setLastPrice(String option, double price) {
        lastPrices.put(option, price);
    }

    public double getLastPrice(String option) {
        // Check the OrderBook's standard trades first, fallback to Mint map if 0
        double bookPrice = orderBooks.get(option).getLastTradePrice();
        return bookPrice > 0 ? bookPrice : lastPrices.getOrDefault(option, 0.0);
    }
}