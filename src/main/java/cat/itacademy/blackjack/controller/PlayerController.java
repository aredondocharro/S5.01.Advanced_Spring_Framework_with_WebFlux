package cat.itacademy.blackjack.controller;

import cat.itacademy.blackjack.dto.PlayerRankingResponse;
import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Register a new player",
            description = "Creates a new player with the given name"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Player successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public Mono<ResponseEntity<PlayerResponse>> registerPlayer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Player data with name",
                    required = true
            )
            @Valid @RequestBody PlayerRequest request
    ) {
        return playerService.create(request)
                .map(player -> ResponseEntity.status(HttpStatus.CREATED).body(player));
    }

    @GetMapping("/name/{name}")
    @Operation(
            summary = "Find player by name",
            description = "Retrieves a player by their name"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Player found"),
            @ApiResponse(responseCode = "404", description = "Player not found")
    })
    public Mono<ResponseEntity<PlayerResponse>> findByName(
            @Parameter(description = "Name of the player", example = "John") @PathVariable String name
    ) {
        return playerService.findByName(name)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/all")
    @Operation(
            summary = "List all players",
            description = "Retrieves a list of all registered players"
    )
    public Flux<PlayerResponse> findAll() {
        return playerService.findAll();
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete player by ID",
            description = "Deletes a player based on their ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Player successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Player not found")
    })
    public Mono<ResponseEntity<Void>> deleteById(
            @Parameter(description = "MongoDB ID of the player", example = "64f1ad1234567890") @PathVariable String id
    ) {
        return playerService.deleteById(id)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/ranking")
    @Operation(summary = "Player ranking", description = "Returns players ranked by win rate and score")
    public Flux<PlayerRankingResponse> getRanking() {
        return playerService.getRanking();
    }
}
