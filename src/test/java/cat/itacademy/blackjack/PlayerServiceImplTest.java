package cat.itacademy.blackjack;

import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.mapper.PlayerMapper;
import cat.itacademy.blackjack.mapper.PlayerMapperImpl;
import cat.itacademy.blackjack.model.Player;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.service.PlayerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        playerRepository = mock(PlayerRepository.class); // ✅ se mockea solo el repo
        playerMapper = new PlayerMapperImpl();            // ✅ se usa la implementación real de MapStruct
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
    void findById_shouldReturnEmpty_whenPlayerNotFound() {
        // Arrange
        String playerId = "not-found";
        Mockito.when(playerRepository.findById(playerId)).thenReturn(Mono.empty());

        // Act & Assert
        playerService.findById(playerId)
                .as(StepVerifier::create)
                .expectComplete()
                .verify();
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
        Flux<PlayerResponse> result = playerService.getRanking();

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(p -> p.name().equals("Bob"))     // 30
                .expectNextMatches(p -> p.name().equals("Charlie")) // 20
                .expectNextMatches(p -> p.name().equals("Alice"))   // 10
                .verifyComplete();

        verify(playerRepository).findAll();
    }
}