package cat.itacademy.blackjack.exception;

import com.mongodb.DuplicateKeyException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Order(1)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponse buildErrorResponse(HttpStatus status, String error, String message, String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path
        );
    }

    @ExceptionHandler({PlayerNotFoundException.class, GameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), exchange.getRequest().getPath().value())
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed JSON", "Malformed JSON or invalid request body", exchange.getRequest().getPath().value())
        );
    }

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(UnsupportedMediaTypeStatusException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
                buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", ex.getMessage(), exchange.getRequest().getPath().value())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, ServerWebExchange exchange) {
        List<FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new FieldError(err.getField(), err.getDefaultMessage()))
                .collect(Collectors.toList());

        ValidationErrorResponse response = new ValidationErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Invalid input data",
                exchange.getRequest().getPath().value(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        return ResponseEntity.badRequest().body(
                buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Error", ex.getMessage(), exchange.getRequest().getPath().value())
        );
    }

    @ExceptionHandler(InvalidInitialCardsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInitialCards(InvalidInitialCardsException ex, ServerWebExchange exchange) {
        return ResponseEntity.badRequest().body(
                buildErrorResponse(HttpStatus.BAD_REQUEST,"Invalid Initial Cards",ex.getMessage(),exchange.getRequest().getPath().value()
                )
        );
    }

    @ExceptionHandler(InvalidPlayerNameException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPlayerName(InvalidPlayerNameException ex, ServerWebExchange exchange) {
        return ResponseEntity.badRequest().body(
                buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid player name", ex.getMessage(), exchange.getRequest().getPath().value())
        );
    }

    @ExceptionHandler(InsufficientCardsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientCards(InsufficientCardsException ex, ServerWebExchange exchange) {
        return ResponseEntity.badRequest().body(
                buildErrorResponse(HttpStatus.BAD_REQUEST, "Insufficient Cards", ex.getMessage(), exchange.getRequest().getPath().value())
        );
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateKeyException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildErrorResponse(HttpStatus.CONFLICT, "Conflict", "A player with that name already exists.", exchange.getRequest().getPath().value())
        );
    }

    @ExceptionHandler(PlayerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePlayerAlreadyExists(PlayerAlreadyExistsException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), exchange.getRequest().getPath().value())
        );
    }

    @ExceptionHandler(InvalidGameStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGameState(InvalidGameStateException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Game State", ex.getMessage(), exchange.getRequest().getPath().value())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, ServerWebExchange exchange) {
        logger.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", exchange.getRequest().getPath().value())
        );
    }
}


