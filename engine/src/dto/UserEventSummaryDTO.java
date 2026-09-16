package dto;

public record UserEventSummaryDTO(
        int eventId,
        String eventName,
        double totalCommissionPaid,
        boolean isClosed,
        Double finalProfitLoss
) {}