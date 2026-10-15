package engine.models;

/** A single LMSR purchase. */
public record LmsrTrade(String userName, String option, int shares, double pricePaid, double commission) {}
