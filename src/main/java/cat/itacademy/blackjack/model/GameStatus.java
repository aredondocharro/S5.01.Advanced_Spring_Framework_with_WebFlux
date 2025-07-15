package cat.itacademy.blackjack.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Possible statuses of a Blackjack game")
public enum GameStatus {
    IN_PROGRESS,
    FINISHED_PLAYER_WON,
    FINISHED_DEALER_WON,
    FINISHED_DRAW
}