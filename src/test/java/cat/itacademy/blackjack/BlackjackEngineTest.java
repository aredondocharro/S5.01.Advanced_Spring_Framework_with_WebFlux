package cat.itacademy.blackjack;

import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.CardSuit;
import cat.itacademy.blackjack.model.CardValue;
import cat.itacademy.blackjack.model.GameStatus;
import cat.itacademy.blackjack.model.TurnResult;
import cat.itacademy.blackjack.service.engine.BlackjackEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlackjackEngineTest {

    private BlackjackEngine engine;

    @BeforeEach
    void setUp() {
        engine = new BlackjackEngine();
    }

    @Test
    void calculateScore_shouldReturnSumOfCardPoints() {
        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardValue.TEN),   // 10
                new Card(CardSuit.SPADES, CardValue.FIVE),  // 5
                new Card(CardSuit.CLUBS, CardValue.TWO)     // 2
        );

        int score = engine.calculateScore(cards);
        assertEquals(17, score);
    }

    @Test
    void determineWinner_shouldReturnPlayerWin() {
        assertEquals(GameStatus.FINISHED_PLAYER_WON, engine.determineWinner(20, 18));
    }

    @Test
    void determineWinner_shouldReturnDealerWin() {
        assertEquals(GameStatus.FINISHED_DEALER_WON, engine.determineWinner(17, 19));
    }

    @Test
    void determineWinner_shouldReturnDraw() {
        assertEquals(GameStatus.FINISHED_DRAW, engine.determineWinner(18, 18));
    }

    @Test
    void determineWinner_shouldReturnDealerWinWhenPlayerBusts() {
        assertEquals(GameStatus.FINISHED_DEALER_WON, engine.determineWinner(22, 18));
    }

    @Test
    void determineWinner_shouldReturnPlayerWinWhenDealerBusts() {
        assertEquals(GameStatus.FINISHED_PLAYER_WON, engine.determineWinner(20, 23));
    }

    @Test
    void simulateTurn_shouldDrawCardsUntilScoreIsAtLeast17() {
        List<Card> deck = new ArrayList<>(List.of(
                new Card(CardSuit.SPADES, CardValue.THREE),  // 3
                new Card(CardSuit.HEARTS, CardValue.FIVE),   // 5
                new Card(CardSuit.DIAMONDS, CardValue.NINE), // 9 → total 17
                new Card(CardSuit.CLUBS, CardValue.TWO)      // won't be drawn
        ));

        TurnResult result = engine.simulateTurn(deck);

        assertTrue(result.score() >= 17, "Score should be >= 17");
        assertEquals(3, result.cards().size(), "Should have drawn 3 cards");
        assertEquals(1, deck.size(), "1 card should be left in deck");
    }

    @Test
    void simulateTurn_shouldStopWhenDeckIsEmpty() {
        List<Card> deck = new ArrayList<>(List.of(
                new Card(CardSuit.HEARTS, CardValue.TEN) // only one card
        ));

        TurnResult result = engine.simulateTurn(deck);

        assertEquals(1, result.cards().size());
        assertEquals(10, result.score());
        assertTrue(deck.isEmpty(), "Deck should be empty after simulation");
    }

    @Test
    void simulateTurn_shouldThrowExceptionIfDeckIsNull() {
        assertThrows(NullPointerException.class, () -> engine.simulateTurn(null));
    }
}
