package cat.itacademy.blackjack.controller;

import cat.itacademy.blackjack.dto.GameRequest;
import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@Tag(name = "Game", description = "Endpoints related to the game of Blackjack")
public class GameController {

    private final GameService gameService;

    @PostMapping("/new")
    @Operation(summary = "Create new game", description = "Creates a new game for a given player.")
    public Mono<ResponseEntity<GameResponse>> createGame(@Valid @RequestBody GameRequest request) {
        return gameService.createGame(request.playerName())
                .map(game -> ResponseEntity.status(HttpStatus.CREATED).body(game));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get game details", description = "Retrieves details of a specific game by its ID.")
    public Mono<ResponseEntity<GameResponse>> getGameById(@PathVariable("id") Long gameId) {
        return gameService.getGameById(gameId)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a game", description = "Deletes a specific game by its ID.")
    public Mono<ResponseEntity<Void>> deleteGame(@PathVariable("id") Long gameId) {
        return gameService.deleteGame(gameId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/all")
    @Operation(summary = "List all games", description = "Returns a list of all games.")
    public Flux<GameResponse> getAllGames() {
        return gameService.getAllGames();
    }

    @PostMapping("/{id}/play")
    @Operation(
            summary = "Play a round of Blackjack",
            description = "Simulates a round of Blackjack for the given game ID. Returns the result, scores, and cards played."
    )
    public Mono<ResponseEntity<GameResponse>> playGame(@PathVariable("id") Long gameId) {
        return gameService.playGame(gameId)
                .map(ResponseEntity::ok);
    }
}

