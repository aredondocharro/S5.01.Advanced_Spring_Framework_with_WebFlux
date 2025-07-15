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
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
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
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createPlayer_returnsSavedPlayer() {
        PlayerRequest request = new PlayerRequest("John");
        Player playerEntity = Player.builder()
                .name("John")
                .totalScore(0)
                .gamesPlayed(0)
                .gamesWon(0)
                .createdAt(LocalDateTime.now())
                .build();
        playerEntity.setId("abc123");

        when(playerRepository.save(any(Player.class))).thenReturn(Mono.just(playerEntity));

        StepVerifier.create(playerService.create(request))
                .expectNextMatches(response ->
                        response.id().equals("abc123") &&
                                response.name().equals("John"))
                .verifyComplete();

        verify(playerRepository, times(1)).save(any(Player.class));
    }

    @Test
    void findById_shouldReturnPlayerResponse_whenPlayerExists() {
        String playerId = "123";
        Player player = Player.builder()
                .id(playerId)
                .name("Jane")
                .totalScore(10)
                .createdAt(LocalDateTime.now())
                .build();

        when(playerRepository.findById(playerId)).thenReturn(Mono.just(player));

        StepVerifier.create(playerService.findById(playerId))
                .expectNextMatches(response ->
                        response.id().equals(playerId) &&
                                response.name().equals("Jane") &&
                                response.totalScore() == 10)
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

        StepVerifier.create(playerService.findAll())
                .expectNextMatches(p -> p.name().equals("Alice"))
                .expectNextMatches(p -> p.name().equals("Bob"))
                .verifyComplete();

        verify(playerRepository).findAll();
    }

    @Test
    void getRanking_ShouldReturnPlayersSortedByScore() {
        Player player1 = Player.builder()
                .id("1")
                .name("Alice")
                .totalScore(10)
                .gamesPlayed(10)
                .gamesWon(5)
                .createdAt(LocalDateTime.now())
                .build();

        Player player2 = Player.builder()
                .id("2")
                .name("Bob")
                .totalScore(30)
                .gamesPlayed(20)
                .gamesWon(15)
                .createdAt(LocalDateTime.now())
                .build();

        Player player3 = Player.builder()
                .id("3")
                .name("Charlie")
                .totalScore(20)
                .gamesPlayed(15)
                .gamesWon(7)
                .createdAt(LocalDateTime.now())
                .build();

        when(playerRepository.findAll()).thenReturn(Flux.just(player1, player2, player3));

        StepVerifier.create(playerService.getRanking())
                .expectNextMatches(p -> p.name().equals("Bob"))     // winRate 15/20=0.75, score=30
                .expectNextMatches(p -> p.name().equals("Charlie")) // winRate 7/15≈0.466
                .expectNextMatches(p -> p.name().equals("Alice"))   // winRate 5/10=0.5 but score lower than Charlie
                .verifyComplete();

        verify(playerRepository).findAll();
    }

    @Test
    void deleteById_shouldDeletePlayerIfExists() {
        String playerId = "abc123";
        Player player = Player.builder()
                .id(playerId)
                .name("DeleteTest")
                .createdAt(LocalDateTime.now())
                .build();

        when(playerRepository.findById(playerId)).thenReturn(Mono.just(player));
        when(playerRepository.delete(player)).thenReturn(Mono.empty());

        StepVerifier.create(playerService.deleteById(playerId))
                .verifyComplete();

        verify(playerRepository).findById(playerId);
        verify(playerRepository).delete(player);
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
    void create_shouldThrowException_whenNameIsNull() {
        PlayerRequest request = new PlayerRequest(null);

        StepVerifier.create(playerService.create(request))
                .expectError(InvalidPlayerNameException.class)
                .verify();
    }

    @Test
    void create_shouldThrowException_whenNameIsEmpty() {
        PlayerRequest request = new PlayerRequest("");

        StepVerifier.create(playerService.create(request))
                .expectError(InvalidPlayerNameException.class)
                .verify();
    }
}

