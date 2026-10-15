package engine.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An event traded with an order book per option (the "Order Book" method). */
public class OrderBookEvent extends Event {
    private static final double EPSILON = 1e-9;

    private final boolean allowMint;
    private final int baseValueD;
    private final int initialInvestment;
    private final Map<String, OrderBook> orderBooks = new LinkedHashMap<>();
    private long nextOrderSequence = 1;

    public OrderBookEvent(String name, String description, int commission, String commissionType,
                          List<String> options, String marketMaker,
                          boolean allowMint, int baseValueD, int initialInvestment) {
        super(name, description, commission, commissionType, options, marketMaker);
        this.allowMint = allowMint;
        this.baseValueD = baseValueD;
        this.initialInvestment = initialInvestment;
        options.forEach(option -> orderBooks.put(option, new OrderBook(option)));
    }

    @Override
    public String getTypeName() { return "Order Book"; }

    /** The market maker buys whole pairs of shares: initial / d pairs, d each. */
    @Override
    public double getActivationCost() { return getInitialSharesPerOption() * (double) baseValueD; }

    @Override
    public double getPayoutPerShare() { return baseValueD; }

    public int getInitialSharesPerOption() { return initialInvestment / baseValueD; }
    public boolean isAllowMint() { return allowMint; }
    public int getBaseValueD() { return baseValueD; }
    public int getInitialInvestment() { return initialInvestment; }
    public OrderBook getOrderBook(String option) { return orderBooks.get(option); }
    public List<OrderBook> getOrderBooks() { return new ArrayList<>(orderBooks.values()); }

    /** The highest price allowed for a single share: d - 0.01. */
    public double getMaxPrice() { return baseValueD - 0.01; }

    public List<Order> getOpenOrdersOf(String userName) {
        List<Order> result = new ArrayList<>();
        for (OrderBook book : orderBooks.values()) {
            book.getBids().stream().filter(o -> o.getUserName().equals(userName)).forEach(result::add);
            book.getAsks().stream().filter(o -> o.getUserName().equals(userName)).forEach(result::add);
        }
        return result;
    }

    /** Shares of the option the user already offered for sale (and are not sold yet). */
    public int getPendingSellShares(String userName, String option) {
        return orderBooks.get(option).getAsks().stream()
                .filter(o -> o.getUserName().equals(userName))
                .mapToInt(Order::getShares).sum();
    }

    /** Money the user committed in buy orders that are still waiting in the book. */
    public double getPendingBuyAmount(String userName) {
        double total = 0;
        for (OrderBook book : orderBooks.values()) {
            for (Order bid : book.getBids()) {
                if (bid.getUserName().equals(userName)) {
                    double amount = bid.getShares() * bid.getPrice();
                    total += amount + (isOnPurchaseCommission() ? calculateCommission(amount) : 0);
                }
            }
        }
        return total;
    }

    public Order createOrder(String userName, String option, boolean buy, int shares, double price) {
        return new Order(nextOrderSequence++, userName, option, buy, shares, price);
    }

    /**
     * Processes a new order: matches it against the book (and mints new shares if allowed).
     * Whatever is not executed stays in the book.
     *
     * @return all the executions that happened, so the caller can settle money and shares.
     */
    public List<Fill> submitOrder(Order incoming) {
        List<Fill> fills = new ArrayList<>();
        OrderBook book = orderBooks.get(incoming.getOption());

        if (incoming.isBuy()) {
            matchBuyAgainstAsks(incoming, book, fills);
            if (allowMint) {
                mintAgainstOppositeBids(incoming, fills);
            }
        } else {
            matchSellAgainstBids(incoming, book, fills);
        }

        orderBooks.values().forEach(OrderBook::removeFilledOrders);
        if (!incoming.isFilled()) {
            book.addOrder(incoming);
        }
        return fills;
    }

    private void matchBuyAgainstAsks(Order bid, OrderBook book, List<Fill> fills) {
        for (Order ask : book.getAsks()) {
            if (bid.isFilled() || ask.getPrice() > bid.getPrice() + EPSILON) {
                break;
            }
            int shares = Math.min(bid.getShares(), ask.getShares());
            // The resting order's price is the trade price
            fills.add(Fill.trade(bid.getUserName(), ask.getUserName(), book.getOption(), shares, ask.getPrice()));
            bid.reduceShares(shares);
            ask.reduceShares(shares);
            book.setLastTradePrice(ask.getPrice());
        }
    }

    private void matchSellAgainstBids(Order ask, OrderBook book, List<Fill> fills) {
        for (Order bid : book.getBids()) {
            if (ask.isFilled() || bid.getPrice() + EPSILON < ask.getPrice()) {
                break;
            }
            int shares = Math.min(ask.getShares(), bid.getShares());
            fills.add(Fill.trade(bid.getUserName(), ask.getUserName(), book.getOption(), shares, bid.getPrice()));
            ask.reduceShares(shares);
            bid.reduceShares(shares);
            book.setLastTradePrice(bid.getPrice());
        }
    }

    /**
     * Mint: a buy order on one option meets a resting buy order on the other option and together
     * they reach d. The resting order pays its full price and the incoming one pays the complement to d.
     * Minting is defined for binary events (exactly two options).
     */
    private void mintAgainstOppositeBids(Order incoming, List<Fill> fills) {
        if (orderBooks.size() != 2) {
            return;
        }
        OrderBook oppositeBook = orderBooks.values().stream()
                .filter(b -> !b.getOption().equals(incoming.getOption()))
                .findFirst().orElseThrow();

        for (Order restingBid : oppositeBook.getBids()) {
            if (incoming.isFilled() || incoming.getPrice() + restingBid.getPrice() + EPSILON < baseValueD) {
                break;
            }
            int shares = Math.min(incoming.getShares(), restingBid.getShares());
            double incomingPrice = baseValueD - restingBid.getPrice();
            fills.add(Fill.mint(incoming.getUserName(), incoming.getOption(), incomingPrice,
                    restingBid.getUserName(), oppositeBook.getOption(), restingBid.getPrice(), shares));
            incoming.reduceShares(shares);
            restingBid.reduceShares(shares);
            orderBooks.get(incoming.getOption()).setLastTradePrice(incomingPrice);
            oppositeBook.setLastTradePrice(restingBid.getPrice());
        }
    }

    public void cancelAllOrders() {
        orderBooks.values().forEach(OrderBook::clear);
    }

    /** The value of a share right now: last trade price, otherwise the mid price, otherwise 0. */
    public double getCurrentShareValue(String option) {
        OrderBook book = orderBooks.get(option);
        if (book.getLastTradePrice() != null) {
            return book.getLastTradePrice();
        }
        return book.getMidPrice() != null ? book.getMidPrice() : 0.0;
    }
}
