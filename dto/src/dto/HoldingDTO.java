package dto;

/** How many shares of an option a user holds, how much he paid for them, and their current value. */
public record HoldingDTO(String option, int shares, double amountPaid, double value) {}
