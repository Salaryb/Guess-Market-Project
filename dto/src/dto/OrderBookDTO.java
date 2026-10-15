package dto;

import java.util.List;

/**
 * The order book of a single option, with its market statistics.
 * A null statistic means "not available" (for example, no trade happened yet).
 */
public record OrderBookDTO(
        String option,
        List<OrderDTO> bids,
        List<OrderDTO> asks,
        Double last,
        Double bid,
        Double ask,
        Double mid,
        Double spread
) {}
