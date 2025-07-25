package cat.itacademy.blackjack.exception;

import java.io.Serial;

public class PlayerNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public PlayerNotFoundException() {
        super();
    }

    public PlayerNotFoundException(String message) {
        super(message);
    }

    public static PlayerNotFoundException forMissingName(String name) {
        return new PlayerNotFoundException("Player with name '" + name + "' not found.");
    }

    public static PlayerNotFoundException forMissingId(String id) {
        return new PlayerNotFoundException("Player with id '" + id + "' not found.");
    }

    public static PlayerNotFoundException forInvalidInput() {
        return new PlayerNotFoundException("Player name must not be null or empty.");
    }
}