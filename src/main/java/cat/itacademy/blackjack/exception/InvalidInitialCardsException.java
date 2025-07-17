package cat.itacademy.blackjack.exception;

public class InvalidInitialCardsException extends RuntimeException {
    public InvalidInitialCardsException(String message) {
        super(message);
    }
}