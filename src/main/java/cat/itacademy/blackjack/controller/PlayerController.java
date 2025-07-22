package cat.itacademy.blackjack.controller;

import cat.itacademy.blackjack.dto.PlayerNameUpdateRequest;
import cat.itacademy.blackjack.dto.PlayerRankingResponse;
import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/player")
@RequiredArgsConstructor
@Tag(name = "Player", description = "Endpoints related to Blackjack players")
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping("/register")
    @Operation(summary = "Register a new player", description = "Creates a new player with the given name")
    public Mono<ResponseEntity<PlayerResponse>> registerPlayer(@Valid @RequestBody PlayerRequest request) {
        return playerService.create(request)
                .map(player -> ResponseEntity.status(HttpStatus.CREATED).body(player));
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Find player by ID", description = "Retrieves a player by their MongoDB ID")
    public Mono<ResponseEntity<PlayerResponse>> findById(@PathVariable String id) {
        return playerService.findById(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Find player by name", description = "Retrieves a player by their name")
    public Mono<ResponseEntity<PlayerResponse>> findByName(@PathVariable String name) {
        return playerService.findByName(name)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete player by ID", description = "Deletes a player based on their ID")
    public Mono<ResponseEntity<Void>> deleteById(@PathVariable String id) {
        return playerService.deleteById(id)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/all")
    @Operation(summary = "List all players", description = "Retrieves a list of all registered players")
    public Flux<PlayerResponse> findAll() {
        return playerService.findAll();
    }

    @GetMapping("/ranking")
    @Operation(summary = "Player ranking", description = "Returns players ranked by win rate and score")
    public Flux<PlayerRankingResponse> getRanking() {
        return playerService.getRanking();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update player name", description = "Updates the name of an existing player by their MongoDB ID")
    public Mono<ResponseEntity<PlayerResponse>> updatePlayerName(
            @PathVariable String id,
            @Valid @RequestBody PlayerNameUpdateRequest request) {
        return playerService.updatePlayerName(id, request.newName())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
