package cat.itacademy.blackjack.service.engine;

import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.CardSuit;
import cat.itacademy.blackjack.model.CardValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DeckManager {

    private static final Logger logger = LoggerFactory.getLogger(DeckManager.class);

    private final ObjectMapper objectMapper;

    public DeckManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Card> generateShuffledDeck() {
        List<Card> deck = new ArrayList<>();
        for (CardSuit suit : CardSuit.values()) {
            for (CardValue value : CardValue.values()) {
                deck.add(Card.builder().suit(suit).value(value).build());
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    public String serializeDeck(List<Card> deck) {
        try {
            return objectMapper.writeValueAsString(deck);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing deck", e);
            throw new RuntimeException("Deck serialization failed", e);
        }
    }

    public Mono<List<Card>> parseDeckReactive(String deckJson) {
        if (deckJson == null || deckJson.isBlank()) {
            return Mono.just(List.of());
        }

        try {
            return Mono.just(objectMapper.readValue(deckJson, new TypeReference<List<Card>>() {}));
        } catch (JsonProcessingException e) {
            logger.error("Error parsing deck JSON", e);
            return Mono.error(new RuntimeException("Deck parsing failed", e));
        }
    }


    public Tuple2<List<Card>, List<Card>> splitDeck(List<Card> deck) {
        List<Card> playerCards = deck.size() >= 2 ? deck.subList(0, 2) : new ArrayList<>(deck);
        List<Card> dealerCards = deck.size() >= 4 ? deck.subList(2, 4) :
                deck.size() > 2 ? deck.subList(2, deck.size()) : new ArrayList<>();

        return Tuples.of(playerCards, dealerCards);
    }
}


