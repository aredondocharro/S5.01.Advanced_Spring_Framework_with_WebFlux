package cat.itacademy.blackjack.model;

public enum CardValue {
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("Jack", 10),
    QUEEN("Queen", 10),
    KING("King", 10),
    ACE("Ace", 11);

    private final String label;
    private final int points;

    CardValue(String label, int points) {
        this.label = label;
        this.points = points;
    }

    public String getLabel() {
        return label;
    }

    public int getPoints() {
        return points;
    }
}