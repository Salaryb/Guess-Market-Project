package dto;

/** A resting (not yet executed) order in an order book. */
public record OrderDTO(String userName, String option, int shares, double price, boolean buy) {}
