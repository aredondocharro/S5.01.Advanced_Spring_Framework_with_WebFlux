package cat.itacademy.blackjack.service.engine;

import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.GameStatus;
import cat.itacademy.blackjack.model.Games;

import java.time.LocalDateTime;
import java.util.List;

public class GameFactory {

    private final DeckManager deckManager;

    public GameFactory(DeckManager deckManager) {
        this.deckManager = deckManager;
    }

    public Games createNewGame(String playerId, List<Card> playerCards, List<Card> dealerCards, List<Card> remainingDeck) {
        int playerScore = calculateScore(playerCards);
        int dealerScore = calculateScore(dealerCards);

        return Games.builder()
                .playerId(playerId)
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .playerScore(playerScore)
                .dealerScore(dealerScore)
                .deckJson(deckManager.serializeDeck(remainingDeck))
                .build();
    }

    public int calculateScore(List<Card> cards) {
        return cards.stream()
                .mapToInt(card -> card.getValue().getPoints())
                .sum();
    }
}

