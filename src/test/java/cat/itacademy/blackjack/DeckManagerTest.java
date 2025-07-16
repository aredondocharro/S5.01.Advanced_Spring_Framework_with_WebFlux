package cat.itacademy.blackjack;

import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.CardSuit;
import cat.itacademy.blackjack.model.CardValue;
import cat.itacademy.blackjack.service.engine.DeckManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeckManagerTest {

    private DeckManager deckManager;

    @BeforeEach
    void setUp() {
        deckManager = new DeckManager(new ObjectMapper());
    }

    @Test
    void generateShuffledDeck_shouldReturn52UniqueCards() {
        List<Card> deck = deckManager.generateShuffledDeck();

        assertEquals(52, deck.size(), "The deck should contain 52 cards");

        Set<String> uniqueCards = new HashSet<>();
        for (Card card : deck) {
            uniqueCards.add(card.getSuit().name() + "-" + card.getValue().name());
        }

        assertEquals(52, uniqueCards.size(), "All cards should be unique");
    }

    @Test
    void serializeAndDeserializeDeck_shouldReturnSameCards() throws JsonProcessingException {
        List<Card> originalDeck = deckManager.generateShuffledDeck();
        String json = deckManager.serializeDeck(originalDeck);

        StepVerifier.create(deckManager.parseDeckReactive(json))
                .assertNext(parsedDeck -> {
                    assertEquals(52, parsedDeck.size(), "Parsed deck should have 52 cards");
                    assertEquals(originalDeck.get(0).getSuit(), parsedDeck.get(0).getSuit());
                    assertEquals(originalDeck.get(0).getValue(), parsedDeck.get(0).getValue());
                })
                .verifyComplete();
    }

    @Test
    void splitDeck_shouldReturnTwoCardsForEach() {
        List<Card> deck = List.of(
                new Card(CardSuit.HEARTS, CardValue.ACE),
                new Card(CardSuit.SPADES, CardValue.FIVE),
                new Card(CardSuit.DIAMONDS, CardValue.TEN),
                new Card(CardSuit.CLUBS, CardValue.JACK)
        );

        Tuple2<List<Card>, List<Card>> result = deckManager.splitDeck(deck);

        assertEquals(2, result.getT1().size(), "Player should have 2 cards");
        assertEquals(2, result.getT2().size(), "Dealer should have 2 cards");
        assertEquals(CardSuit.HEARTS, result.getT1().get(0).getSuit());
        assertEquals(CardSuit.SPADES, result.getT1().get(1).getSuit());
        assertEquals(CardSuit.DIAMONDS, result.getT2().get(0).getSuit());
        assertEquals(CardSuit.CLUBS, result.getT2().get(1).getSuit());
    }
}
