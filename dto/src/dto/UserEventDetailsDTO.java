package dto;

import java.util.List;

/** The involvement of a specific user in a specific event. */
public record UserEventDetailsDTO(
        String eventName,
        String type,
        String status,
        boolean marketMaker,
        List<String> options,
        List<HoldingDTO> holdings,
        List<TradeDTO> trades,
        List<OrderDTO> openOrders,
        double commissionPaid,
        Double profitLoss,
        String winningOption,
        List<OptionStatusDTO> totalsPerOption
) {}
