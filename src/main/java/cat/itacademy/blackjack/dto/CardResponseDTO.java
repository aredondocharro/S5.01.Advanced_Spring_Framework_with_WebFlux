package cat.itacademy.blackjack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO representing a playing card")
public record CardResponseDTO(
        @Schema(description = "Suit of the card (e.g., HEARTS, DIAMONDS)") String suit,
        @Schema(description = "Value of the card (e.g., ACE, KING, TWO)") String value
) {}