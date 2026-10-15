package dto;

import java.util.List;

/** General (summary) information about a single event, as shown in the events table. */
public record EventDTO(
        String name,
        String description,
        String status,
        String type,
        int commission,
        String commissionType,
        double accountBalance,
        String marketMaker,
        List<String> options
) {}
