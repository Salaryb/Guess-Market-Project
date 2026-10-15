package dto;

import java.util.List;

/** Chat lines starting from a requested index, plus the current chat version (= total number of lines). */
public record ChatDTO(int version, List<ChatLineDTO> lines) {}
