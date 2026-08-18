package dto;

import java.util.List;

public record EventDetailsDTO(EventDTO event, double accountBalance, double totalCommission, List<TradeDTO> trades, List<Double> currentPrices) {}