package cat.itacademy.blackjack.dto;

import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.GameStatus;
import java.time.LocalDateTime;
import java.util.List;

public record GameResponse(
        Long id,
        String playerId,
        LocalDateTime createdAt,
        GameStatus status,
        int playerScore,
        int dealerScore,
        List<Card> playerCards,
        List<Card> dealerCards
) {}