package cat.itacademy.blackjack.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Possible turn statuses of a Blackjack game")
public enum GameTurn {
    PLAYER_TURN,
    FINISHED
}