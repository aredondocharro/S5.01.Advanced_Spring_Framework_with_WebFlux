package cat.itacademy.blackjack.dto;

import jakarta.validation.constraints.NotBlank;

public record PlayerNameUpdateRequest(
        @NotBlank(message = "New player name cannot be empty")
        String newName
) {}