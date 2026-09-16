package engine.models;
import java.util.List;

public abstract class Event {
    protected int id;
    protected String name;
    protected String description;
    protected double commission;
    protected String commissionType;
    protected boolean isActive = false;
    protected boolean isClosed = false;
    protected double accountBalance = 0.0;
    protected double totalCommissionCollected = 0.0;
    protected List<String> optionNames;
    protected String winningOption = null;

    public Event(int id, String name, String description, double commission, String commissionType, List<String> optionNames) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.optionNames = optionNames;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return isActive; }
    public boolean isClosed() { return isClosed; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setClosed(boolean closed) { this.isClosed = closed; }
    public double getAccountBalance() { return accountBalance; }
    public String getWinningOption() { return winningOption; }
    public double getTotalCommissionCollected() { return totalCommissionCollected; }
    public void addToAccountBalance(double amount) { this.accountBalance += amount; }
    public void emptyAccount() { this.accountBalance = 0; }
    public List<String> getOptionNames() { return optionNames; }
    public String getCommissionType() { return commissionType; }
    public double getCommission() { return commission; }
    public void setWinningOption(String option) { this.winningOption = option; }

    public abstract boolean isOrderBook();

    public void addCommission(double amount) {
        this.totalCommissionCollected += amount;
    }
}