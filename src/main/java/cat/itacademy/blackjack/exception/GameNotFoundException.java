package cat.itacademy.blackjack.exception;

import java.io.Serial;

public class GameNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public GameNotFoundException() {
        super();
    }

    public GameNotFoundException(Long id) {
        super("Game with id '" + id + "' not found.");
    }

    public GameNotFoundException(String message) {
        super(message);
    }
}