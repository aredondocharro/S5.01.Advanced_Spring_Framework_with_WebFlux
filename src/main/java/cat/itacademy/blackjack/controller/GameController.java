package cat.itacademy.blackjack.controller;

import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.model.Game;
import cat.itacademy.blackjack.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@Tag(name = "Game", description = "Endpoints related to the game of Blackjack")
public class GameController {

    private final GameService gameService;

    @PostMapping("/new")
    @Operation(summary = "Create new game", description = "Creates a new game for a given player.")
    public Mono<ResponseEntity<Game>> createGame(@RequestBody PlayerRequest request) {
        return gameService.createGame(request.name())
                .map(game -> ResponseEntity.status(HttpStatus.CREATED).body(game));
    }
}
