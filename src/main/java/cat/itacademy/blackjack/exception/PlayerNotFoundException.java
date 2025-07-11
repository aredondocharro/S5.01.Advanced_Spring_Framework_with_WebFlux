package cat.itacademy.blackjack.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String message) {
        super(message);
    }

    public static PlayerNotFoundException forMissingName(String name) {
        return new PlayerNotFoundException("Player with name '" + name + "' not found.");
    }

    public static PlayerNotFoundException forInvalidInput() {
        return new PlayerNotFoundException("Player name must not be null or empty.");
    }
}