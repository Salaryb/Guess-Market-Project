package engine.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Event implements Serializable{
    private static final long serialVersionUID = 1L;
    private final int id;
    private final String name;
    private final String description;
    private final int commission;
    private final String commissionType;
    private final List<String> optionNames;
    private final int[] sharesBought;
    private final int b;
    private Integer winningOptionIndex = null;

    private boolean isActive;
    private double accountBalance;
    private double totalCommission;
    private final List<Trade> tradeHistory;

    public Event(int id, String name, String description, int commission, String commissionType, List<String> optionNames, int b) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.optionNames = optionNames;
        this.sharesBought = new int[optionNames.size()];
        this.b = b;
        this.isActive = true;
        this.accountBalance = 0;
        this.totalCommission = 0;
        this.tradeHistory = new ArrayList<>();
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommission() { return commission; }
    public String getCommissionType() { return commissionType; }
    public List<String> getOptionNames() { return optionNames; }
    public int getSharesBought(int index) { return sharesBought[index]; }
    public int getB() { return b; }
    public boolean isActive() { return isActive; }
    public double getAccountBalance() { return accountBalance; }
    public double getTotalCommission() { return totalCommission; }
    public List<Trade> getTradeHistory() { return tradeHistory; }
    public Integer getWinningOptionIndex() { return winningOptionIndex; }

    public void addFunds(double amount) { this.accountBalance += amount; }
    public void deductFunds(double amount) { this.accountBalance -= amount; }
    public void addCommission(double amount) {
        this.totalCommission += amount;
        this.accountBalance += amount;
    }
    public void addShares(int optionIndex, int amount) { this.sharesBought[optionIndex] += amount; }
    public void recordTrade(Trade trade) { this.tradeHistory.add(trade); }
    public void setActive(boolean active) { this.isActive = active; }
    public void setWinningOptionIndex(Integer winningOptionIndex) { this.winningOptionIndex = winningOptionIndex; }
}