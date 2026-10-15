package dto;

/** A single LMSR purchase line. */
public record TradeDTO(String userName, String option, int shares, double pricePaid, double commission) {}
