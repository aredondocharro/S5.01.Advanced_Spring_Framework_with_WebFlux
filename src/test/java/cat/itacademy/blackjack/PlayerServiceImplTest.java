package cat.itacademy.blackjack;

import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.exception.InvalidPlayerNameException;
import cat.itacademy.blackjack.exception.PlayerAlreadyExistsException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.PlayerMapper;
import cat.itacademy.blackjack.model.Player;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.service.PlayerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;


import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceImplTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private PlayerServiceImpl playerService;

    private PlayerRequest validRequest;
    private Player samplePlayer;
    private PlayerResponse sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = new PlayerRequest("John");
        samplePlayer = new Player("id123", "John", 100, 10, 5, LocalDateTime.now());
        sampleResponse = new PlayerResponse("id123", "John", 100, samplePlayer.getCreatedAt());
    }

    @Test
    void create_ShouldFail_WhenNameIsNull() {
        StepVerifier.create(playerService.create(new PlayerRequest(null)))
                .expectError(InvalidPlayerNameException.class)
                .verify();
    }

    @Test
    void create_ShouldFail_WhenNameIsEmpty() {
        StepVerifier.create(playerService.create(new PlayerRequest("   ")))
                .expectError(InvalidPlayerNameException.class)
                .verify();
    }

    @Test
    void create_ShouldFail_WhenPlayerAlreadyExists() {
        when(playerRepository.findByName("John")).thenReturn(Mono.just(samplePlayer));

        StepVerifier.create(playerService.create(validRequest))
                .expectError(PlayerAlreadyExistsException.class)
                .verify();
    }

    @Test
    void create_ShouldSucceed_WhenPlayerIsNew() {
        when(playerRepository.findByName("John")).thenReturn(Mono.empty());
        when(playerMapper.toEntity(validRequest)).thenReturn(samplePlayer);
        when(playerRepository.save(samplePlayer)).thenReturn(Mono.just(samplePlayer));
        when(playerMapper.toResponse(samplePlayer)).thenReturn(sampleResponse);

        StepVerifier.create(playerService.create(validRequest))
                .expectNext(sampleResponse)
                .verifyComplete();
    }

    @Test
    void findByName_ShouldSucceed_WhenPlayerExists() {
        when(playerRepository.findByName("John")).thenReturn(Mono.just(samplePlayer));
        when(playerMapper.toResponse(samplePlayer)).thenReturn(sampleResponse);

        StepVerifier.create(playerService.findByName("John"))
                .expectNext(sampleResponse)
                .verifyComplete();
    }

    @Test
    void findByName_ShouldFail_WhenPlayerNotFound() {
        when(playerRepository.findByName("John")).thenReturn(Mono.empty());

        StepVerifier.create(playerService.findByName("John"))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }

    @Test
    void findById_ShouldSucceed_WhenPlayerExists() {
        when(playerRepository.findById("id123")).thenReturn(Mono.just(samplePlayer));
        when(playerMapper.toResponse(samplePlayer)).thenReturn(sampleResponse);

        StepVerifier.create(playerService.findById("id123"))
                .expectNext(sampleResponse)
                .verifyComplete();
    }

    @Test
    void findById_ShouldFail_WhenPlayerNotFound() {
        when(playerRepository.findById("id123")).thenReturn(Mono.empty());

        StepVerifier.create(playerService.findById("id123"))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }

    @Test
    void findAll_ShouldReturnListOfPlayers() {
        Player another = new Player("id2", "Alice", 80, 8, 4, LocalDateTime.now());
        PlayerResponse response2 = new PlayerResponse("id2", "Alice", 80, another.getCreatedAt());

        when(playerRepository.findAll()).thenReturn(Flux.just(samplePlayer, another));
        when(playerMapper.toResponse(samplePlayer)).thenReturn(sampleResponse);
        when(playerMapper.toResponse(another)).thenReturn(response2);

        StepVerifier.create(playerService.findAll())
                .expectNext(sampleResponse)
                .expectNext(response2)
                .verifyComplete();
    }

    @Test
    void getRanking_ShouldReturnOrderedRanking() {
        Player player1 = new Player("1", "A", 100, 10, 5, LocalDateTime.now()); // 50% winRate
        Player player2 = new Player("2", "B", 200, 10, 8, LocalDateTime.now()); // 80% winRate

        when(playerRepository.findAll()).thenReturn(Flux.just(player1, player2));

        StepVerifier.create(playerService.getRanking())
                .expectNextMatches(r -> r.name().equals("B") && r.winRate() == 0.8)
                .expectNextMatches(r -> r.name().equals("A") && r.winRate() == 0.5)
                .verifyComplete();
    }

    @Test
    void deleteById_ShouldFail_WhenIdIsNullOrEmpty() {
        StepVerifier.create(playerService.deleteById(null))
                .expectError(PlayerNotFoundException.class)
                .verify();

        StepVerifier.create(playerService.deleteById("   "))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }

    @Test
    void deleteById_ShouldFail_WhenPlayerNotFound() {
        when(playerRepository.findById("id123")).thenReturn(Mono.empty());

        StepVerifier.create(playerService.deleteById("id123"))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }

    @Test
    void deleteById_ShouldSucceed_WhenPlayerExists() {
        when(playerRepository.findById("id123")).thenReturn(Mono.just(samplePlayer));
        when(playerRepository.delete(samplePlayer)).thenReturn(Mono.empty());

        StepVerifier.create(playerService.deleteById("id123"))
                .verifyComplete();

        verify(playerRepository).delete(samplePlayer);
    }
    @Test
    void updatePlayerName_ShouldSucceed_WhenPlayerExistsAndNameIsNew() {
        Player updatedPlayer = new Player("id123", "updatedPlayer", 100, 10, 5, samplePlayer.getCreatedAt());
        PlayerResponse updatedResponse = new PlayerResponse("id123", "updatedPlayer", 100, samplePlayer.getCreatedAt());

        when(playerRepository.findByName("updatedPlayer")).thenReturn(Mono.empty());
        when(playerRepository.findById("id123")).thenReturn(Mono.just(samplePlayer));
        when(playerRepository.save(any(Player.class))).thenReturn(Mono.just(updatedPlayer));
        when(playerMapper.toResponse(updatedPlayer)).thenReturn(updatedResponse);

        StepVerifier.create(playerService.updatePlayerName("id123", "updatedPlayer"))
                .expectNextMatches(response -> response.id().equals("id123") && response.name().equals("updatedPlayer"))
                .verifyComplete();

        verify(playerRepository).save(argThat(player -> player.getName().equals("updatedPlayer")));
    }

    @Test
    void updatePlayerName_ShouldFail_WhenIdIsNullOrEmpty() {
        StepVerifier.create(playerService.updatePlayerName(null, "updatedPlayer"))
                .expectErrorMatches(throwable -> throwable instanceof PlayerNotFoundException &&
                        throwable.getMessage().equals("Player ID must not be null or empty."))
                .verify();

        StepVerifier.create(playerService.updatePlayerName("   ", "updatedPlayer"))
                .expectErrorMatches(throwable -> throwable instanceof PlayerNotFoundException &&
                        throwable.getMessage().equals("Player ID must not be null or empty."))
                .verify();
    }

    @Test
    void updatePlayerName_ShouldFail_WhenNameIsNullOrEmpty() {
        StepVerifier.create(playerService.updatePlayerName("id123", null))
                .expectErrorMatches(throwable -> throwable instanceof InvalidPlayerNameException &&
                        throwable.getMessage().equals("New player name cannot be null or empty"))
                .verify();

        StepVerifier.create(playerService.updatePlayerName("id123", "   "))
                .expectErrorMatches(throwable -> throwable instanceof InvalidPlayerNameException &&
                        throwable.getMessage().equals("New player name cannot be null or empty"))
                .verify();
    }

    @Test
    void updatePlayerName_ShouldFail_WhenPlayerNotFound() {
        when(playerRepository.findByName("updatedPlayer")).thenReturn(Mono.empty());
        when(playerRepository.findById("id123")).thenReturn(Mono.empty());

        StepVerifier.create(playerService.updatePlayerName("id123", "updatedPlayer"))
                .expectErrorMatches(throwable -> throwable instanceof PlayerNotFoundException &&
                        throwable.getMessage().equals("Player with id 'id123' not found."))
                .verify();
    }

    @Test
    void updatePlayerName_ShouldFail_WhenNameAlreadyExists() {
        Player existingPlayer = new Player("anotherId", "updatedPlayer", 50, 5, 2, LocalDateTime.now());

        when(playerRepository.findByName("updatedPlayer")).thenReturn(Mono.just(existingPlayer));

        StepVerifier.create(playerService.updatePlayerName("id123", "updatedPlayer"))
                .expectErrorMatches(throwable -> throwable instanceof PlayerAlreadyExistsException &&
                        throwable.getMessage().equals("Player with name 'updatedPlayer' already exists"))
                .verify();
    }
}

