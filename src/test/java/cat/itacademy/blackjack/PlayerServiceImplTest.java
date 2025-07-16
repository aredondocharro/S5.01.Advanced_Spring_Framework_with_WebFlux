package cat.itacademy.blackjack;

import cat.itacademy.blackjack.dto.PlayerRankingResponse;
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
}

