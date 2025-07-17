package cat.itacademy.blackjack.gameservice;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.InsufficientCardsException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.*;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.service.GameServiceImpl;
import cat.itacademy.blackjack.service.engine.BlackjackEngine;
import cat.itacademy.blackjack.service.engine.DeckManager;
import cat.itacademy.blackjack.service.logic.GameCreationService;
import cat.itacademy.blackjack.service.logic.GameHitProcessor;
import cat.itacademy.blackjack.service.logic.GameStandProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceImplTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private GameMapper gameMapper;
    @Mock
    private DeckManager deckManager;
    @Mock
    private BlackjackEngine blackjackEngine;
    @Mock
    private GameCreationService gameCreationService;
    @Mock
    private GameHitProcessor gameHitProcessor;
    @Mock
    private GameStandProcessor gameStandProcessor;

    @InjectMocks
    private GameServiceImpl gameService;

    private Player player;
    private Games game;

    @BeforeEach
    void setUp() {
        player = new Player("playerId", "John", 100, 10, 5, null);
        game = new Games();
        game.setId(1L);
        game.setPlayerId("playerId");
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setDeckJson("[]"); // simplificado
    }

    // --- createGame ---
    @Test
    void createGame_shouldFail_whenPlayerNameIsNull() {
        when(gameCreationService.createGame(null))
                .thenReturn(Mono.error(new PlayerNotFoundException("Player with name 'null' not found.")));

        StepVerifier.create(gameService.createGame(null))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }


    @Test
    void createGame_shouldFail_whenPlayerNotFound() {
        when(gameCreationService.createGame("John"))
                .thenReturn(Mono.error(new PlayerNotFoundException("Player with name 'John' not found.")));

        StepVerifier.create(gameService.createGame("John"))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }


    @Test
    void createGame_shouldFail_whenDeckIsInsufficient() {
        when(gameCreationService.createGame("John"))
                .thenReturn(Mono.error(new InsufficientCardsException("Not enough cards in the deck to start a game")));

        StepVerifier.create(gameService.createGame("John"))
                .expectError(InsufficientCardsException.class)
                .verify();
    }


    // --- getGameById ---
    @Test
    void getGameById_shouldFail_whenIdIsNull() {
        StepVerifier.create(gameService.getGameById(null))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void getGameById_shouldFail_whenGameNotFound() {
        when(gameRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(gameService.getGameById(1L))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void getGameById_shouldSucceed() {
        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));
        when(deckManager.deserializeCardsReactive(anyString()))
                .thenReturn(Mono.just(List.of(new Card(CardSuit.HEARTS, CardValue.EIGHT))));
        when(deckManager.splitDeck(anyList()))
                .thenReturn(Tuples.of(List.of(), List.of()));
        when(gameMapper.toResponse(eq(game), anyList(), anyList()))
                .thenReturn(new GameResponse(
                        1L,
                        "playerId",
                        LocalDateTime.now(),
                        GameStatus.IN_PROGRESS,
                        GameTurn.PLAYER_TURN,
                        18,
                        17,
                        List.of(),
                        List.of()
                ));

        StepVerifier.create(gameService.getGameById(1L))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getAllGames_shouldReturnAll() {
        when(gameRepository.findAll()).thenReturn(Flux.just(game));
        when(deckManager.deserializeCardsReactive(anyString())).thenReturn(Mono.just(List.of()));
        when(deckManager.splitDeck(anyList())).thenReturn(Tuples.of(List.of(), List.of()));
        when(gameMapper.toResponse(eq(game), anyList(), anyList()))
                .thenReturn(new GameResponse(
                        1L,
                        "playerId",
                        LocalDateTime.now(),
                        GameStatus.IN_PROGRESS,
                        GameTurn.PLAYER_TURN,
                        18,
                        17,
                        List.of(),
                        List.of()
                ));

        StepVerifier.create(gameService.getAllGames())
                .expectNextCount(1)
                .verifyComplete();
    }

    // --- deleteGame ---
    @Test
    void deleteGame_shouldFail_whenIdIsNull() {
        StepVerifier.create(gameService.deleteGame(null))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void deleteGame_shouldFail_whenGameNotFound() {
        when(gameRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(gameService.deleteGame(1L))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void deleteGame_shouldSucceed() {
        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));
        when(gameRepository.delete(game)).thenReturn(Mono.empty());

        StepVerifier.create(gameService.deleteGame(1L))
                .verifyComplete();

        verify(gameRepository).delete(game);
    }

    // --- GameLogic ---
    @Test
    void hit_shouldFail_whenIdIsNull() {
        when(gameHitProcessor.processHit(null))
                .thenReturn(Mono.error(new GameNotFoundException("Game ID must not be null")));

        StepVerifier.create(gameService.hit(null))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void hit_shouldReturnProcessedResponse() {
        Long gameId = 1L;
        GameResponse mockResponse = mock(GameResponse.class);

        when(gameHitProcessor.processHit(gameId)).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(gameService.hit(gameId))
                .expectNext(mockResponse)
                .verifyComplete();
    }


    @Test
    void stand_shouldFail_whenIdIsNull() {
        when(gameStandProcessor.processStand(null))
                .thenReturn(Mono.error(new GameNotFoundException("Game ID must not be null")));

        StepVerifier.create(gameService.stand(null))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void stand_shouldReturnProcessedResponse() {
        Long gameId = 1L;
        GameResponse mockResponse = mock(GameResponse.class);

        when(gameStandProcessor.processStand(gameId)).thenReturn(Mono.just(mockResponse));

        StepVerifier.create(gameService.stand(gameId))
                .expectNext(mockResponse)
                .verifyComplete();
    }
}
