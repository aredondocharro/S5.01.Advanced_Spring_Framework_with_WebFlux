package cat.itacademy.blackjack.dto;

import jakarta.validation.constraints.NotBlank;

public record PlayerRequest(
        @NotBlank(message = "Player name cannot be empty")
        String name
) {}