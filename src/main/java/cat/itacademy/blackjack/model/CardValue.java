package cat.itacademy.blackjack.model;

import lombok.Getter;

@Getter
public enum CardValue {
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(10),
    QUEEN(10),
    KING( 10),
    ACE(11);

    private final int points;

    CardValue(int points) {

        this.points = points;
    }

}