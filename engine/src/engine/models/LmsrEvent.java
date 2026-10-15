package engine.models;

import engine.LMSRMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An event traded with the LMSR method (supports any number of options). */
public class LmsrEvent extends Event {
    private final int b;
    private final Map<String, Integer> sharesPerOption = new LinkedHashMap<>();
    private final List<LmsrTrade> trades = new ArrayList<>();

    public LmsrEvent(String name, String description, int commission, String commissionType,
                     List<String> options, String marketMaker, int b) {
        super(name, description, commission, commissionType, options, marketMaker);
        this.b = b;
        options.forEach(option -> sharesPerOption.put(option, 0));
    }

    @Override
    public String getTypeName() { return "LMSR"; }

    /** The initial subsidy: C(0,...,0) = b * ln(number of options). */
    @Override
    public double getActivationCost() {
        return LMSRMath.calculateCost(new ArrayList<>(sharesPerOption.values()), b);
    }

    @Override
    public double getPayoutPerShare() { return 1.0; }

    public int getB() { return b; }

    public int getSharesBought(String option) { return sharesPerOption.get(option); }

    public double getPrice(String option) {
        List<Integer> quantities = new ArrayList<>(sharesPerOption.values());
        int index = new ArrayList<>(sharesPerOption.keySet()).indexOf(option);
        return LMSRMath.calculatePrice(quantities, index, b);
    }

    /** The cost (before commission) of buying the given amount of shares of the option right now. */
    public double calculatePurchaseCost(String option, int shares) {
        List<Integer> before = new ArrayList<>(sharesPerOption.values());
        List<Integer> after = new ArrayList<>();
        sharesPerOption.forEach((opt, qty) -> after.add(opt.equals(option) ? qty + shares : qty));
        return LMSRMath.calculateCost(after, b) - LMSRMath.calculateCost(before, b);
    }

    public void recordPurchase(LmsrTrade trade) {
        sharesPerOption.merge(trade.option(), trade.shares(), Integer::sum);
        trades.add(trade);
    }

    /** Trades ordered from the latest to the first. */
    public List<LmsrTrade> getTradesLatestFirst() {
        List<LmsrTrade> result = new ArrayList<>(trades);
        Collections.reverse(result);
        return result;
    }
}
