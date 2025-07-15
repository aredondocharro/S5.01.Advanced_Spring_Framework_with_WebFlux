package cat.itacademy.blackjack.exception;

import java.io.Serial;

public class InsufficientCardsException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InsufficientCardsException() {
        super();
    }

    public InsufficientCardsException(String message) {
        super(message);
    }
}