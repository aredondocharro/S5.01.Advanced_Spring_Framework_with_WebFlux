package cat.itacademy.blackjack;

import cat.itacademy.blackjack.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GamesTest {

    @Test
    void builder_shouldSetAllFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.now();

        Games game = Games.builder()
                .id(1L)
                .playerId("player123")
                .createdAt(now)
                .status(GameStatus.IN_PROGRESS)
                .playerScore(19)
                .dealerScore(18)
                .deckJson("json")
                .playerCards(List.of(new Card(CardSuit.HEARTS, CardValue.ACE)))
                .dealerCards(List.of(new Card(CardSuit.SPADES, CardValue.TEN)))
                .build();

        assertEquals(1L, game.getId());
        assertEquals("player123", game.getPlayerId());
        assertEquals(now, game.getCreatedAt());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(19, game.getPlayerScore());
        assertEquals(18, game.getDealerScore());
        assertEquals("json", game.getDeckJson());
        assertEquals(1, game.getPlayerCards().size());
        assertEquals(CardSuit.HEARTS, game.getPlayerCards().get(0).getSuit());
    }

    @Test
    void settersAndGetters_shouldWork() {
        Games game = new Games();
        game.setId(2L);
        game.setPlayerId("player456");

        assertEquals(2L, game.getId());
        assertEquals("player456", game.getPlayerId());
    }
}
