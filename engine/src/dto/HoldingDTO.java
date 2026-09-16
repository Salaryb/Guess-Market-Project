package dto;

public record HoldingDTO(
        String optionName,
        int shares,
        double investedAmount
) {}