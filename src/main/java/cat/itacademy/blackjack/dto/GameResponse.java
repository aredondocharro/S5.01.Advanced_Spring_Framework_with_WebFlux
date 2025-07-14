package cat.itacademy.blackjack.dto;

import cat.itacademy.blackjack.model.GameStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Response DTO for a Blackjack game")
public record GameResponse(
        Long id,
        String playerId,
        LocalDateTime createdAt,
        GameStatus status,
        int playerScore,
        int dealerScore,
        List<CardResponseDTO> playerCards,
        List<CardResponseDTO> dealerCards
) {}