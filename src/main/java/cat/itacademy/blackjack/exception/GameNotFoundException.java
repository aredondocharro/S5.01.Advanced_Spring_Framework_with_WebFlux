package cat.itacademy.blackjack.exception;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(Long id) {
        super("Game with id '" + id + "' not found.");
    }
    public GameNotFoundException(String message) {
        super(message);
    }
}