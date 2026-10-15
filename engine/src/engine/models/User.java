package engine.models;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A user of the system: his account (balance + every transaction), his shares in every event,
 * and the money that flowed in/out per event (used for profit / loss).
 */
public class User {
    private final String name;
    private double balance = 0.0;
    private boolean online = true;
    private final List<AccountTransaction> transactions = new ArrayList<>();
    private final Set<String> marketMakerOf = new LinkedHashSet<>();
    private final Map<String, Map<String, Integer>> holdings = new HashMap<>();
    private final Map<String, Map<String, Double>> amountPaidPerOption = new HashMap<>();
    private final Map<String, Double> commissionPaid = new HashMap<>();
    private final Map<String, Double> eventCashFlow = new HashMap<>();

    public User(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isBlocked() { return balance < 0; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public List<AccountTransaction> getTransactions() { return Collections.unmodifiableList(transactions); }

    public boolean isMarketMaker() { return !marketMakerOf.isEmpty(); }
    public boolean isMarketMakerOf(String eventName) { return marketMakerOf.contains(eventName); }
    public void addMarketMakerEvent(String eventName) { marketMakerOf.add(eventName); }

    /**
     * Changes the balance and records a transaction line.
     * @param eventName the event this money belongs to (for profit / loss), or null.
     */
    public void changeBalance(double amount, String type, String description, String eventName) {
        double before = balance;
        balance += amount;
        transactions.add(new AccountTransaction(transactions.size() + 1, LocalTime.now(),
                type, description, amount, before, balance));
        if (eventName != null) {
            eventCashFlow.merge(eventName, amount, Double::sum);
        }
    }

    public void addShares(String eventName, String option, int shares, double amountPaid) {
        holdings.computeIfAbsent(eventName, k -> new LinkedHashMap<>()).merge(option, shares, Integer::sum);
        amountPaidPerOption.computeIfAbsent(eventName, k -> new LinkedHashMap<>()).merge(option, amountPaid, Double::sum);
    }

    /** Removes sold shares; the amount paid is reduced by the average cost of the removed shares. */
    public void removeShares(String eventName, String option, int shares) {
        int owned = getShares(eventName, option);
        double averageCost = owned > 0 ? getAmountPaid(eventName, option) / owned : 0.0;
        addShares(eventName, option, -shares, -averageCost * shares);
    }

    public void addCommissionPaid(String eventName, double amount) {
        commissionPaid.merge(eventName, amount, Double::sum);
    }

    public int getShares(String eventName, String option) {
        return holdings.getOrDefault(eventName, Map.of()).getOrDefault(option, 0);
    }

    public double getAmountPaid(String eventName, String option) {
        return amountPaidPerOption.getOrDefault(eventName, Map.of()).getOrDefault(option, 0.0);
    }

    public boolean hasShares(String eventName) {
        return holdings.getOrDefault(eventName, Map.of()).values().stream().anyMatch(qty -> qty > 0);
    }

    public double getCommissionPaid(String eventName) { return commissionPaid.getOrDefault(eventName, 0.0); }
    public double getEventCashFlow(String eventName) { return eventCashFlow.getOrDefault(eventName, 0.0); }
}
