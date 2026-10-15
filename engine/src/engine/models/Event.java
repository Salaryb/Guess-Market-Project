package engine.models;

import exception.GuessMarketException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Common data and behaviour of every event, regardless of its trading method. */
public abstract class Event {
    public static final String ON_CLOSE = "on-close";
    public static final String ON_PURCHASE = "on-purchase";

    private final String name;
    private final String description;
    private final int commission;
    private final String commissionType;
    private final List<String> options;
    private final String marketMaker;
    private final Set<String> participants = new LinkedHashSet<>();

    private EventStatus status = EventStatus.NOT_STARTED;
    private double accountBalance = 0.0;
    private double totalCommissionCollected = 0.0;
    private String winningOption = null;

    protected Event(String name, String description, int commission, String commissionType,
                    List<String> options, String marketMaker) {
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.options = List.copyOf(options);
        this.marketMaker = marketMaker;
    }

    public abstract String getTypeName();

    /** The amount the market maker has to pay into the event account when activating it. */
    public abstract double getActivationCost();

    /** The amount paid at the end of the event for every share of the winning option. */
    public abstract double getPayoutPerShare();

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommission() { return commission; }
    public String getCommissionType() { return commissionType; }
    public List<String> getOptions() { return options; }
    public String getMarketMaker() { return marketMaker; }
    public EventStatus getStatus() { return status; }
    public double getAccountBalance() { return accountBalance; }
    public double getTotalCommissionCollected() { return totalCommissionCollected; }
    public String getWinningOption() { return winningOption; }
    public Set<String> getParticipants() { return Collections.unmodifiableSet(participants); }

    public boolean isActive() { return status == EventStatus.ACTIVE; }
    public boolean isEnded() { return status == EventStatus.ENDED; }
    public boolean isOnPurchaseCommission() { return ON_PURCHASE.equals(commissionType); }
    public boolean isOnCloseCommission() { return ON_CLOSE.equals(commissionType); }

    public double calculateCommission(double amount) {
        return amount * commission / 100.0;
    }

    public void activate() { status = EventStatus.ACTIVE; }

    public void end(String winningOption) {
        this.winningOption = winningOption;
        this.status = EventStatus.ENDED;
    }

    public void addParticipant(String userName) { participants.add(userName); }
    public void addToAccount(double amount) { accountBalance += amount; }
    public void addCommission(double amount) { totalCommissionCollected += amount; }

    /** Finds the option matching the given text (case insensitive) or throws a clear error. */
    public String resolveOption(String optionText) {
        if (optionText != null) {
            for (String option : options) {
                if (option.equalsIgnoreCase(optionText.trim())) {
                    return option;
                }
            }
        }
        throw new GuessMarketException("Option '" + optionText + "' does not exist in event '" + name
                + "'. Valid options are: " + String.join(", ", options));
    }
}
