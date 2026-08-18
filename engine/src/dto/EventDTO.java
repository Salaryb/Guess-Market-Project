package dto;

import java.util.List;

public record EventDTO(int id, String name, String description, int commission, String commissionType, boolean isActive, List<OptionDTO> options, Integer winningOptionIndex) {}