package dto;

public record EventDTO(
        int id,
        String name,
        String status,
        String type,
        double commission,
        String commissionType
) {}