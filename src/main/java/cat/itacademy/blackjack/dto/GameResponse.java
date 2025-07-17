package cat.itacademy.blackjack.dto;

import cat.itacademy.blackjack.model.GameStatus;
import cat.itacademy.blackjack.model.GameTurn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Response DTO for a Blackjack game")
public record GameResponse(

        @Schema(description = "Unique game ID", example = "123")
        Long id,

        @Schema(description = "MongoDB player ID", example = "64d7a98dfb13")
        String playerId,

        @Schema(description = "Game creation timestamp", type = "string", format = "date-time", example = "2025-07-14T19:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Status of the game", example = "IN_PROGRESS")
        GameStatus status,

        @Schema(description = "Current turn in the game", example = "PLAYER_TURN")
        GameTurn turn, // ✅ NUEVO CAMPO

        @Schema(description = "Total score of the player", example = "19")
        int playerScore,

        @Schema(description = "Total score of the dealer", example = "18")
        int dealerScore,

        @ArraySchema(schema = @Schema(implementation = CardResponseDTO.class),
                arraySchema = @Schema(description = "List of cards held by the player"))
        List<CardResponseDTO> playerCards,

        @ArraySchema(schema = @Schema(implementation = CardResponseDTO.class),
                arraySchema = @Schema(description = "List of cards held by the dealer"))
        List<CardResponseDTO> dealerCards

) {}
