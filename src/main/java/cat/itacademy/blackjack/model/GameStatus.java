package cat.itacademy.blackjack.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Possible statuses of a Blackjack game")
public enum GameStatus {
    IN_PROGRESS,
    PLAYER_WON,
    DEALER_WON,
    FINISHED,
    DRAW
}