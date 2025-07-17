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

    @GetMapping("/details/{id}")
    @Operation(summary = "Get game details", description = "Retrieves details of a specific game by its ID.")
    public Mono<ResponseEntity<GameResponse>> getGameById(@PathVariable("id") Long gameId) {
        return gameService.getGameById(gameId)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/delete/{id}")
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


@PostMapping("/{id}/hit")
@Operation(summary = "Hit", description = "Plays a player turn.")
public Mono<ResponseEntity<GameResponse>> hit(@PathVariable Long id) {
    return gameService.hit(id)
            .map(ResponseEntity::ok);
}

    @PostMapping("/{id}/stand")
    @Operation(summary = "Stand", description = "Player stands and the dealer plays the  turn.")
    public Mono<ResponseEntity<GameResponse>> stand(@PathVariable Long id) {
        return gameService.stand(id)
                .map(ResponseEntity::ok);
    }
}

