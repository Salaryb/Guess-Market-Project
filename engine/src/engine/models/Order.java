package engine.models;

/** A buy or sell order in an order book. Shares are reduced as the order gets executed. */
public class Order {
    private final long sequence;
    private final String userName;
    private final String option;
    private final boolean buy;
    private final double price;
    private int shares;

    public Order(long sequence, String userName, String option, boolean buy, int shares, double price) {
        this.sequence = sequence;
        this.userName = userName;
        this.option = option;
        this.buy = buy;
        this.shares = shares;
        this.price = price;
    }

    public long getSequence() { return sequence; }
    public String getUserName() { return userName; }
    public String getOption() { return option; }
    public boolean isBuy() { return buy; }
    public double getPrice() { return price; }
    public int getShares() { return shares; }
    public boolean isFilled() { return shares == 0; }

    public void reduceShares(int amount) { shares -= amount; }
}
