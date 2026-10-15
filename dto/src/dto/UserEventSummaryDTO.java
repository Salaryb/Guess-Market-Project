package dto;

/** An event as seen from a specific user's point of view (his role in it). */
public record UserEventSummaryDTO(String eventName, String type, String status, String role) {}
