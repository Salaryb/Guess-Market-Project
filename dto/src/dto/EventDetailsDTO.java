package dto;

import java.util.List;

/**
 * Full details of a single event (the "drill down" view).
 * LMSR events fill lmsrOptions + lmsrTrades, Order Book events fill orderBooks.
 */
public record EventDetailsDTO(
        EventDTO event,
        double totalCommission,
        String winningOption,
        List<OptionStatusDTO> lmsrOptions,
        List<TradeDTO> lmsrTrades,
        List<OrderBookDTO> orderBooks,
        List<ParticipantDTO> participants
) {}
