package engine.models;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class User {
    private final String name;
    private double balance;
    private boolean isBlocked = false;
    private final Set<Integer> marketMakerEvents;
    private final Map<Integer, Map<String, Integer>> holdings = new HashMap<>();
    private final Map<Integer, Double> eventCashFlow = new HashMap<>();
    private final Map<Integer, Double> eventCommissions = new HashMap<>();

    public User(String name, double balance, Set<Integer> marketMakerEvents) {
        this.name = name;
        this.balance = balance;
        this.marketMakerEvents = marketMakerEvents;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isBlocked() { return isBlocked; }
    public boolean isMMFor(int eventId) { return marketMakerEvents.contains(eventId); }

    public void updateBalance(double amount) {
        this.balance += amount;
        if (this.balance < 0) this.isBlocked = true;
    }

    public void addHoldings(int eventId, String option, int shares) {
        holdings.putIfAbsent(eventId, new HashMap<>());
        Map<String, Integer> eventHoldings = holdings.get(eventId);
        eventHoldings.put(option, eventHoldings.getOrDefault(option, 0) + shares);
    }

    public void recordTrade(int eventId, String option, int sharesDelta, double moneyDelta, double commission) {
        addHoldings(eventId, option, sharesDelta);
        eventCashFlow.put(eventId, eventCashFlow.getOrDefault(eventId, 0.0) + moneyDelta);
        eventCommissions.put(eventId, eventCommissions.getOrDefault(eventId, 0.0) + commission);
        updateBalance(moneyDelta - commission);
    }

    public Map<String, Integer> getEventHoldings(int eventId) {
        return holdings.getOrDefault(eventId, new HashMap<>());
    }

    public double getEventCashFlow(int eventId) {
        return eventCashFlow.getOrDefault(eventId, 0.0);
    }

    public void addEventCashFlow(int eventId, double amount) {
        eventCashFlow.put(eventId, getEventCashFlow(eventId) + amount);
    }
}