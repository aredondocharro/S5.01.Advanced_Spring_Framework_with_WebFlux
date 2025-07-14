package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.CardSuit;
import cat.itacademy.blackjack.model.CardValue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
public class DeckService {

    public List<Card> getShuffledDeck() {
        List<Card> deck = new ArrayList<>();
        for (CardSuit suit : CardSuit.values()) {
            for (CardValue value : CardValue.values()) {
                deck.add(Card.builder().suit(suit).value(value).build());
            }
        }
        Collections.shuffle(deck);
        return deck;
    }
}
