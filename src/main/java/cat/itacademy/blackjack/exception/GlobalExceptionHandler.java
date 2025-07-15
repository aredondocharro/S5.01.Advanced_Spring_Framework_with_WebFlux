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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maneja de forma global las excepciones lanzadas por la API,
 * devolviendo respuestas HTTP con formato consistente y mensajes claros.
 */
@RestControllerAdvice
@Order(1)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler({PlayerNotFoundException.class, GameNotFoundException.class})
    public ResponseEntity<Object> handleNotFound(RuntimeException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), exchange);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleInvalidJson(HttpMessageNotReadableException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed JSON or invalid request body", exchange);
    }


    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(UnsupportedMediaTypeStatusException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", "Unsupported Content-Type: " + ex.getMessage(), exchange);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationErrors(MethodArgumentNotValidException ex, ServerWebExchange exchange) {
        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    Map<String, String> err = new HashMap<>();
                    err.put("field", error.getField());
                    err.put("message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "Validation error");
                    return err;
                })
                .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Error");
        body.put("messages", errors);
        body.put("path", exchange.getRequest().getPath().value());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Error", ex.getMessage(), exchange);
    }


    @ExceptionHandler(InvalidPlayerNameException.class)
    public ResponseEntity<Object> handleInvalidPlayerName(InvalidPlayerNameException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid player name", ex.getMessage(), exchange);
    }


    @ExceptionHandler(InsufficientCardsException.class)
    public ResponseEntity<Object> handleInsufficientCards(InsufficientCardsException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Insufficient Cards", ex.getMessage(), exchange);
    }


    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Object> handleDuplicateKey(DuplicateKeyException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", "A player with that name already exists.", exchange);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, ServerWebExchange exchange) {
        logger.error("Unexpected error occurred", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", exchange);
    }

    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String errorTitle, String message, ServerWebExchange exchange) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", errorTitle);
        error.put("message", message);
        error.put("path", exchange.getRequest().getPath().value());
        return new ResponseEntity<>(error, status);
    }
}

