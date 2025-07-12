package cat.itacademy.blackjack.model;

import java.util.List;

public record TurnResult(int score, List<Card> cards) {}