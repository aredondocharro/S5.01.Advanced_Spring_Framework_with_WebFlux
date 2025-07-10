package cat.itacademy.blackjack.controller;

import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "Endpoints for managing players")
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    @Operation(summary = "Create new player")
    public Mono<ResponseEntity<PlayerResponse>> createPlayer(@RequestBody PlayerRequest request) {
        return playerService.create(request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get player by ID")
    public Mono<ResponseEntity<PlayerResponse>> getById(@PathVariable String id) {
        return playerService.findById(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    @Operation(summary = "Get all players")
    public Flux<PlayerResponse> getAll() {
        return playerService.findAll();
    }

    @GetMapping("/ranking")
    @Operation(summary = "Get player ranking (by score)")
    public Flux<PlayerResponse> getRanking() {
        return playerService.getRanking();
    }
}
