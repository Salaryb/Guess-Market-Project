package dto;

/** LMSR: the current price (0..1) of an option and the total shares bought from it so far. */
public record OptionStatusDTO(String option, double price, int sharesBought) {}
