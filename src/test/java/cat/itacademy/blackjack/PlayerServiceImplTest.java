package cat.itacademy.blackjack;

import cat.itacademy.blackjack.dto.PlayerRankingResponse;
import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.exception.InvalidPlayerNameException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.PlayerMapper;
import cat.itacademy.blackjack.mapper.PlayerMapperImpl;
import cat.itacademy.blackjack.model.Player;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.service.PlayerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

class PlayerServiceImplTest {

    private PlayerRepository playerRepository;
    private PlayerMapper playerMapper;
    private PlayerServiceImpl playerService;

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        playerMapper = new PlayerMapperImpl();
        playerService = new PlayerServiceImpl(playerRepository, playerMapper);
    }

    @Test
    void createPlayer_returnsSavedPlayer() {
        // Arrange
        PlayerRequest request = new PlayerRequest("John");
        Player playerEntity = Player.builder()
                .name("John")
                .totalScore(0)
                .gamesPlayed(0)
                .gamesWon(0)
                .createdAt(LocalDateTime.now())
                .build();
        playerEntity.setId("abc123");
        playerEntity.setCreatedAt(LocalDateTime.now());

        when(playerRepository.save(any(Player.class)))
                .thenReturn(Mono.just(playerEntity));

        // Act
        Mono<PlayerResponse> result = playerService.create(request);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.id().equals("abc123") &&
                                response.name().equals("John")
                )
                .verifyComplete();

        verify(playerRepository, times(1)).save(any(Player.class));
    }

    @Test
    void findById_shouldReturnPlayerResponse_whenPlayerExists() {
        // Arrange
        String playerId = "123";
        Player player = Player.builder()
                .id(playerId)
                .name("Jane")
                .totalScore(10)
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(playerRepository.findById(playerId)).thenReturn(Mono.just(player));

        // Act & Assert
        playerService.findById(playerId)
                .as(StepVerifier::create)
                .expectNextMatches(response ->
                        response.id().equals(playerId) &&
                                response.name().equals("Jane") &&
                                response.totalScore() == 10
                )
                .verifyComplete();
    }

    @Test
    void findById_shouldThrowException_whenPlayerNotFound() {
        String playerId = "not-found";
        when(playerRepository.findById(playerId)).thenReturn(Mono.empty());

        StepVerifier.create(playerService.findById(playerId))
                .expectErrorMatches(error ->
                        error instanceof PlayerNotFoundException &&
                                error.getMessage().equals("Player with id 'not-found' not found."))
                .verify();

        verify(playerRepository).findById(playerId);
    }

    @Test
    void findAll_ShouldReturnAllPlayers() {
        // Arrange
        Player player1 = Player.builder()
                .id("1")
                .name("Alice")
                .totalScore(10)
                .createdAt(LocalDateTime.now())
                .build();

        Player player2 = Player.builder()
                .id("2")
                .name("Bob")
                .totalScore(20)
                .createdAt(LocalDateTime.now())
                .build();

        when(playerRepository.findAll()).thenReturn(Flux.just(player1, player2));

        // Act
        Flux<PlayerResponse> result = playerService.findAll();

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(p -> p.name().equals("Alice"))
                .expectNextMatches(p -> p.name().equals("Bob"))
                .verifyComplete();

        verify(playerRepository).findAll();
    }

    @Test
    void getRanking_ShouldReturnPlayersSortedByScore() {
        // Arrange
        Player player1 = Player.builder()
                .id("1")
                .name("Alice")
                .totalScore(10)
                .createdAt(LocalDateTime.now())
                .build();

        Player player2 = Player.builder()
                .id("2")
                .name("Bob")
                .totalScore(30)
                .createdAt(LocalDateTime.now())
                .build();

        Player player3 = Player.builder()
                .id("3")
                .name("Charlie")
                .totalScore(20)
                .createdAt(LocalDateTime.now())
                .build();

        // Se simula un orden no clasificado desde el repositorio
        when(playerRepository.findAll()).thenReturn(Flux.just(player1, player2, player3));

        // Act
        Flux<PlayerRankingResponse> result = playerService.getRanking();

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(p -> p.name().equals("Bob"))     // 30
                .expectNextMatches(p -> p.name().equals("Charlie")) // 20
                .expectNextMatches(p -> p.name().equals("Alice"))   // 10
                .verifyComplete();

        verify(playerRepository).findAll();
    }

    @Test
    void deleteById_shouldDeletePlayerIfExists() {
        // Arrange
        String playerId = "abc123";
        Player player = Player.builder()
                .id(playerId)
                .name("DeleteTest")
                .createdAt(LocalDateTime.now())
                .build();

        when(playerRepository.findById(playerId)).thenReturn(Mono.just(player));
        when(playerRepository.delete(player)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(playerService.deleteById(playerId))
                .verifyComplete();

        verify(playerRepository).findById(playerId);
        verify(playerRepository).delete(player);
    }

    @Test
    void registerPlayer_shouldReturnExistingPlayer() {
        String name = "ExistingPlayer";
        Player existing = Player.builder()
                .id("abc123")
                .name(name)
                .totalScore(0)
                .gamesPlayed(0)
                .gamesWon(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(playerRepository.findByName(name)).thenReturn(Mono.just(existing));

        StepVerifier.create(playerService.registerPlayer(name))
                .expectNextMatches(response ->
                        response.name().equals(name) &&
                                response.id().equals("abc123"))
                .verifyComplete();

        verify(playerRepository).findByName(name);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void registerPlayer_shouldCreateNewPlayerWhenNotExists() {
        String name = "NewPlayer";

        when(playerRepository.findByName(name)).thenReturn(Mono.empty());

        when(playerRepository.save(any(Player.class)))
                .thenAnswer(invocation -> {
                    Player toSave = invocation.getArgument(0);
                    toSave.setId("new123");
                    return Mono.just(toSave);
                });

        StepVerifier.create(playerService.registerPlayer(name))
                .expectNextMatches(response ->
                        response.name().equals(name) &&
                                response.id().equals("new123"))
                .verifyComplete();

        verify(playerRepository).findByName(name);
        verify(playerRepository).save(any());
    }

    @Test
    void findByName_shouldReturnPlayer_whenExists() {
        String name = "Alice";
        Player player = Player.builder()
                .id("a1")
                .name(name)
                .totalScore(42)
                .createdAt(LocalDateTime.now())
                .build();

        when(playerRepository.findByName(name)).thenReturn(Mono.just(player));

        StepVerifier.create(playerService.findByName(name))
                .expectNextMatches(response ->
                        response.name().equals("Alice") &&
                                response.totalScore() == 42)
                .verifyComplete();

        verify(playerRepository).findByName(name);
    }
    @Test
    void findByName_shouldThrowException_whenNotFound() {
        String missingName = "ghost";

        when(playerRepository.findByName(missingName)).thenReturn(Mono.empty());

        StepVerifier.create(playerService.findByName(missingName))
                .expectErrorSatisfies(error -> {
                    assertThat(error)
                            .isInstanceOf(PlayerNotFoundException.class)
                            .hasMessage("Player with name '" + missingName + "' not found.");
                })
                .verify();

        verify(playerRepository).findByName(missingName);
    }

    @Test
    void registerPlayer_shouldThrowWhenNameIsNull() {
        StepVerifier.create(playerService.registerPlayer(null))
                .expectError(InvalidPlayerNameException.class)
                .verify();
    }

    @Test
    void registerPlayer_shouldThrowWhenNameIsEmpty() {
        StepVerifier.create(playerService.registerPlayer(""))
                .expectError(InvalidPlayerNameException.class)
                .verify();
    }

}