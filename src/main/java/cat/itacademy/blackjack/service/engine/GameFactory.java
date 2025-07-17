package cat.itacademy.blackjack.service.engine;

import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.GameStatus;
import cat.itacademy.blackjack.model.Games;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class GameFactory {

    private final DeckManager deckManager;
    private final BlackjackEngine blackjackEngine;

    public GameFactory(DeckManager deckManager, BlackjackEngine blackjackEngine) {
        this.deckManager = deckManager;
        this.blackjackEngine = blackjackEngine;
    }

    public Games createNewGame(String playerId, List<Card> playerCards, List<Card> dealerCards, List<Card> remainingDeck) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(playerCards, "Player cards cannot be null");
        Objects.requireNonNull(dealerCards, "Dealer cards cannot be null");
        Objects.requireNonNull(remainingDeck, "Remaining deck cannot be null");

        if (playerCards.size() != 2) {
            throw new IllegalArgumentException("Player must have exactly 2 cards");
        }

        if (dealerCards.size() != 2) {
            throw new IllegalArgumentException("Dealer must have exactly 2 cards");
        }

        int playerScore = blackjackEngine.calculateScore(playerCards);
        int dealerScore = blackjackEngine.calculateScore(dealerCards);

        return Games.builder()
                .playerId(playerId)
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .playerScore(playerScore)
                .dealerScore(dealerScore)
                .deckJson(deckManager.serializeDeck(remainingDeck))
                .playerCardsJson(deckManager.serializeDeck(playerCards))
                .dealerCardsJson(deckManager.serializeDeck(dealerCards))
                .build();
    }
}



