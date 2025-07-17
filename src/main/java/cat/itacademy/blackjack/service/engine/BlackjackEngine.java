package cat.itacademy.blackjack.service.engine;

import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.GameStatus;
import cat.itacademy.blackjack.model.TurnResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class BlackjackEngine {

    private static final Logger logger = LoggerFactory.getLogger(BlackjackEngine.class);

    public TurnResult simulateTurn(List<Card> deck) {
        Objects.requireNonNull(deck, "Deck cannot be null");

        List<Card> cards = new ArrayList<>();
        int score = 0;

        while (score < 17 && !deck.isEmpty()) {
            Card card = deck.remove(0);
            cards.add(card);
            score = calculateScore(cards);
        }

        logger.debug("Turn simulated. Cards: {}, Score: {}", cards, score);
        return new TurnResult(score, cards);
    }

    public int calculateScore(List<Card> cards) {
        return cards.stream()
                .mapToInt(card -> card.getValue().getPoints())
                .sum();
    }

    public GameStatus determineWinner(int playerScore, int dealerScore) {
        if (playerScore > 21) return GameStatus.FINISHED_DEALER_WON;
        if (dealerScore > 21) return GameStatus.FINISHED_PLAYER_WON;
        if (playerScore > dealerScore) return GameStatus.FINISHED_PLAYER_WON;
        if (dealerScore > playerScore) return GameStatus.FINISHED_DEALER_WON;
        return GameStatus.FINISHED_DRAW;
    }

    public TurnResult simulateTurnWithInitial(List<Card> initialCards, List<Card> deck) {
        Objects.requireNonNull(deck, "Deck cannot be null");

        List<Card> cards = new ArrayList<>(initialCards);
        int score = calculateScore(cards);

        while (score < 17 && !deck.isEmpty()) {
            Card card = deck.remove(0);
            cards.add(card);
            score = calculateScore(cards);
        }

        logger.debug("Dealer turn simulated. Cards: {}, Score: {}", cards, score);
        return new TurnResult(score, cards);
    }

}

