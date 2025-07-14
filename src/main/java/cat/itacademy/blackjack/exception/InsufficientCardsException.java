package cat.itacademy.blackjack.exception;

public class InsufficientCardsException extends RuntimeException {
    public InsufficientCardsException(String message) {
        super(message);
    }
}