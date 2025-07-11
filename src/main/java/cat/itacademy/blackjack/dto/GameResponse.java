package cat.itacademy.blackjack.dto;

import cat.itacademy.blackjack.model.GameStatus;
import java.time.LocalDateTime;

public record GameResponse(
        Long id,
        String playerId,
        LocalDateTime createdAt,
        GameStatus status,
        int playerScore,
        int dealerScore
) {}