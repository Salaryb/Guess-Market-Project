package dto;

import java.util.List;

/** Private information of the logged-in user. */
public record UserDetailsDTO(
        String name,
        double balance,
        boolean blocked,
        List<UserEventSummaryDTO> events,
        List<TransactionDTO> transactions
) {}
