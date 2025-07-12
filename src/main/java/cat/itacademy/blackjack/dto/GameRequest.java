package cat.itacademy.blackjack.dto;

import jakarta.validation.constraints.NotBlank;

public record GameRequest(
        @NotBlank(message = "Player name must not be blank")
        String playerName
) {}