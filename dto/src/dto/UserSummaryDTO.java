package dto;

/** Public information about a user, visible to all other users. */
public record UserSummaryDTO(String name, double balance, boolean marketMaker, boolean online) {}
