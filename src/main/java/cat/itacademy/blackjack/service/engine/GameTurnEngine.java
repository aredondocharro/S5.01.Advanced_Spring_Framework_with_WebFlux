package cat.itacademy.blackjack.service.engine;

import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.TurnResult;

import java.util.ArrayList;
import java.util.List;

public class GameTurnEngine {

    public TurnResult simulateTurn(List<Card> deck) {
        List<Card> cards = new ArrayList<>();
        int score = 0;

        while (score < 17 && !deck.isEmpty()) {
            Card card = deck.remove(0);
            cards.add(card);
            score = calculateScore(cards);
        }

        return new TurnResult(score, cards);
    }

    public int calculateScore(List<Card> cards) {
        return cards.stream()
                .mapToInt(card -> card.getValue().getPoints())
                .sum();
    }
}

