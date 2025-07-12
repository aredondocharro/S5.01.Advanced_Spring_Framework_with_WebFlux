package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.CardSuit;
import cat.itacademy.blackjack.model.CardValue;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DeckService {

    private final List<Card> deck = new ArrayList<>();

    @PostConstruct
    public void initializeDeck() {
        for (CardSuit suit : CardSuit.values()) {
            for (CardValue value : CardValue.values()) {
                deck.add(Card.builder()
                        .suit(suit)
                        .value(value)
                        .build());
            }
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(deck);
    }

    public Card drawCard() {
        if (deck.isEmpty()) {
            initializeDeck();
        }
        return deck.remove(ThreadLocalRandom.current().nextInt(deck.size()));
    }
}
