package cat.itacademy.blackjack;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.InsufficientCardsException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.CardMapper;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.*;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.service.GameServiceImpl;
import cat.itacademy.blackjack.service.engine.BlackjackEngine;
import cat.itacademy.blackjack.service.engine.DeckManager;
import cat.itacademy.blackjack.service.engine.GameFactory;
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
    private CardMapper cardMapper;
    @Mock
    private DeckManager deckManager;
    @Mock
    private GameFactory gameFactory;
    @Mock
    private BlackjackEngine blackjackEngine;

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
        StepVerifier.create(gameService.createGame(null))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }

    @Test
    void createGame_shouldFail_whenPlayerNotFound() {
        when(playerRepository.findByName("John")).thenReturn(Mono.empty());

        StepVerifier.create(gameService.createGame("John"))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }

    @Test
    void createGame_shouldFail_whenDeckIsInsufficient() {
        when(playerRepository.findByName("John")).thenReturn(Mono.just(player));
        when(deckManager.generateShuffledDeck()).thenReturn(List.of()); // < 4

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
        when(deckManager.parseDeckReactive(anyString()))
                .thenReturn(Mono.just(List.of(new Card(CardSuit.HEARTS, CardValue.EIGHT))));
        when(deckManager.splitDeck(anyList()))
                .thenReturn(Tuples.of(List.of(), List.of()));
        when(gameMapper.toResponse(eq(game), anyList(), anyList()))
                .thenReturn(new GameResponse(
                        1L,
                        "playerId",
                        LocalDateTime.now(),
                        GameStatus.IN_PROGRESS,
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
        when(deckManager.parseDeckReactive(anyString())).thenReturn(Mono.just(List.of()));
        when(deckManager.splitDeck(anyList())).thenReturn(Tuples.of(List.of(), List.of()));
        when(gameMapper.toResponse(eq(game), anyList(), anyList()))
                .thenReturn(new GameResponse(
                        1L,
                        "playerId",
                        LocalDateTime.now(),
                        GameStatus.IN_PROGRESS,
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

    // --- playGame ---
    @Test
    void playGame_shouldFail_whenIdIsNull() {
        StepVerifier.create(gameService.playGame(null))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void playGame_shouldFail_whenGameNotFound() {
        when(gameRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(gameService.playGame(1L))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void playGame_shouldSimulateGameAndReturnUpdatedGameResponse() {
        // Setup game en progreso
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setDeckJson("[]");

        List<Card> deck = List.of(
                new Card(CardSuit.HEARTS, CardValue.FIVE),
                new Card(CardSuit.SPADES, CardValue.SIX),
                new Card(CardSuit.CLUBS, CardValue.THREE),
                new Card(CardSuit.DIAMONDS, CardValue.TWO)
        );

        TurnResult playerTurn = new TurnResult(18, List.of(deck.get(0), deck.get(1)));
        TurnResult dealerTurn = new TurnResult(16, List.of(deck.get(2), deck.get(3)));

        // Simulación de ganador
        GameStatus result = GameStatus.FINISHED_PLAYER_WON;
        String updatedDeckJson = "[{\"suit\":\"SPADES\",\"value\":\"TEN\"}]";

        // Simular lógica de repositorios y servicios
        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));
        when(deckManager.parseDeckReactive(anyString())).thenReturn(Mono.just(deck));
        when(blackjackEngine.simulateTurn(deck)).thenReturn(playerTurn).thenReturn(dealerTurn);
        when(blackjackEngine.determineWinner(18, 16)).thenReturn(result);
        when(deckManager.serializeDeck(deck)).thenReturn(updatedDeckJson);
        when(gameRepository.save(any(Games.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(playerRepository.findById("playerId")).thenReturn(Mono.just(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(gameMapper.toResponse(any(Games.class), anyList(), anyList()))
                .thenReturn(new GameResponse(
                        1L,
                        "playerId",
                        LocalDateTime.now(),
                        result,
                        playerTurn.score(),
                        dealerTurn.score(),
                        List.of(),  // mock playerCards
                        List.of()   // mock dealerCards
                ));

        StepVerifier.create(gameService.playGame(1L))
                .expectNextMatches(response ->
                        response.status() == result &&
                                response.playerScore() == 18 &&
                                response.dealerScore() == 16
                )
                .verifyComplete();

        // Verifica que el jugador fue actualizado correctamente
        verify(playerRepository).save(argThat(p ->
                p.getGamesPlayed() == 11 &&
                        p.getGamesWon() == 6 &&
                        p.getTotalScore() == 118 &&
                        p.getName().equals("John")
        ));
    }
    @Test
    void playGame_shouldReturnGameWithoutPlaying_whenStatusIsNotInProgress() {
        game.setStatus(GameStatus.FINISHED_PLAYER_WON); // Juego ya terminado
        game.setDeckJson("[]");

        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));
        when(deckManager.parseDeckReactive(anyString())).thenReturn(Mono.just(List.of()));
        when(deckManager.splitDeck(anyList())).thenReturn(Tuples.of(List.of(), List.of()));
        when(gameMapper.toResponse(eq(game), anyList(), anyList()))
                .thenReturn(new GameResponse(
                        1L, "playerId", LocalDateTime.now(),
                        GameStatus.FINISHED_PLAYER_WON, 20, 18,
                        List.of(), List.of()
                ));

        StepVerifier.create(gameService.playGame(1L))
                .expectNextMatches(response -> response.status() == GameStatus.FINISHED_PLAYER_WON)
                .verifyComplete();

        verify(playerRepository, never()).save(any());
    }
    @Test
    void playGame_shouldFail_whenDeckHasLessThan4Cards() {
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setDeckJson("[]");

        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));
        when(deckManager.parseDeckReactive(anyString())).thenReturn(Mono.just(List.of(
                new Card(CardSuit.HEARTS, CardValue.TWO),
                new Card(CardSuit.CLUBS, CardValue.FIVE) // Solo 2 cartas
        )));

        StepVerifier.create(gameService.playGame(1L))
                .expectError(InsufficientCardsException.class)
                .verify();
    }
}
