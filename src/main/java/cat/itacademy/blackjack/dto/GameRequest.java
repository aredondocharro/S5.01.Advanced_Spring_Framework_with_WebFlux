package cat.itacademy.blackjack.dto;

import jakarta.validation.constraints.NotBlank;

public record GameRequest(
        @NotBlank String playerName
) {}
