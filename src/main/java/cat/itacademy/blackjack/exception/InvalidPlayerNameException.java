package cat.itacademy.blackjack.exception;

import java.io.Serial;

public class InvalidPlayerNameException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidPlayerNameException() {
        super();
    }

    public InvalidPlayerNameException(String message) {
        super(message);
    }
}