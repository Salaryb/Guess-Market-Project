package dto;

import java.util.List;

/** A participant of an event (holds shares or has an order in the book). */
public record ParticipantDTO(String name, List<HoldingDTO> holdings, int openOrders, double totalValue) {}
