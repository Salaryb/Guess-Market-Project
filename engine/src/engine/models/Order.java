package engine.models;
import java.time.LocalDateTime;

public class Order implements Comparable<Order> {
    private final String userName;
    private final boolean isBuy;
    private int shares;
    private final double price;
    private final LocalDateTime timestamp;

    public Order(String userName, boolean isBuy, int shares, double price) {
        this.userName = userName;
        this.isBuy = isBuy;
        this.shares = shares;
        this.price = price;
        this.timestamp = LocalDateTime.now();
    }

    public void reduceShares(int amount) { this.shares -= amount; }
    public String getUserName() { return userName; }
    public boolean isBuy() { return isBuy; }
    public int getShares() { return shares; }
    public double getPrice() { return price; }

    @Override
    public int compareTo(Order other) {
        int priceCompare = Double.compare(this.price, other.price);
        if (priceCompare != 0) {
            return this.isBuy ? -priceCompare : priceCompare;
        }
        return this.timestamp.compareTo(other.timestamp);
    }
}