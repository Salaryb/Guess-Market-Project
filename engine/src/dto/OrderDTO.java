package dto;

public record OrderDTO(
        String userName,
        String option,
        int shares,
        double price,
        boolean isBuy
) {}