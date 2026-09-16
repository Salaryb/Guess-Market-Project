package engine.models;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LmsrEvent extends Event {
    private final int b;
    private final List<Trade> trades = new ArrayList<>();
    private final Map<String, Integer> optionShares = new HashMap<>();

    public LmsrEvent(int id, String name, String description, double commission, String commissionType, List<String> optionNames, int b) {
        super(id, name, description, commission, commissionType, optionNames);
        this.b = b;
    }

    @Override
    public boolean isOrderBook() { return false; }
    public int getB() { return b; }
    public List<Trade> getTrades() { return trades; }
    public Map<String, Integer> getOptionShares() { return optionShares; }
}