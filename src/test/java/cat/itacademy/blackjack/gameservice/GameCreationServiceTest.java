package cat.itacademy.blackjack.gameservice;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.InsufficientCardsException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.*;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.service.engine.BlackjackEngine;
import cat.itacademy.blackjack.service.engine.DeckManager;
import cat.itacademy.blackjack.service.engine.GameFactory;
import cat.itacademy.blackjack.service.logic.GameCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GameCreationServiceTest {

    private PlayerRepository playerRepository;
    private GameRepository gameRepository;
    private DeckManager deckManager;
    private GameFactory gameFactory;
    private GameMapper gameMapper;
    private BlackjackEngine blackjackEngine;
    private GameCreationService gameCreationService;

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        gameRepository = mock(GameRepository.class);
        deckManager = mock(DeckManager.class);
        gameFactory = mock(GameFactory.class);
        gameMapper = mock(GameMapper.class);
        blackjackEngine = mock(BlackjackEngine.class);

        gameCreationService = new GameCreationService(
                playerRepository,
                gameRepository,
                deckManager,
                gameFactory,
                gameMapper,
                blackjackEngine
        );
    }

    @Test
    void createGame_shouldFail_whenPlayerNameIsNull() {
        StepVerifier.create(gameCreationService.createGame(null))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }

    @Test
    void createGame_shouldFail_whenPlayerNameIsEmpty() {
        StepVerifier.create(gameCreationService.createGame(""))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }

    @Test
    void createGame_shouldFail_whenPlayerNotFound() {
        when(playerRepository.findByName("John")).thenReturn(Mono.empty());

        StepVerifier.create(gameCreationService.createGame("John"))
                .expectError(PlayerNotFoundException.class)
                .verify();
    }

    @Test
    void createGame_shouldFail_whenDeckIsInsufficient() {
        Player player = new Player("1", "John", 0, 0, 0, LocalDateTime.now());
        when(playerRepository.findByName("John")).thenReturn(Mono.just(player));
        when(deckManager.generateShuffledDeck()).thenReturn(List.of(new Card(CardSuit.HEARTS, CardValue.FIVE))); // < 4

        StepVerifier.create(gameCreationService.createGame("John"))
                .expectError(InsufficientCardsException.class)
                .verify();
    }

    @Test
    void createGame_shouldSucceed_whenPlayerExistsAndDeckIsValid() {
        // Arrange
        Player player = new Player("1", "John", 0, 0, 0, LocalDateTime.now());

        List<Card> deck = new ArrayList<>(List.of(
                new Card(CardSuit.HEARTS, CardValue.TWO),
                new Card(CardSuit.SPADES, CardValue.THREE),
                new Card(CardSuit.CLUBS, CardValue.FOUR),
                new Card(CardSuit.DIAMONDS, CardValue.FIVE),
                new Card(CardSuit.SPADES, CardValue.SIX)
        ));

        List<Card> playerCards = List.of(deck.get(0), deck.get(1));
        List<Card> dealerCards = List.of(deck.get(2), deck.get(3));

        Games mockGame = new Games();
        mockGame.setId(1L);
        mockGame.setPlayerId(player.getId());
        mockGame.setCreatedAt(LocalDateTime.now());

        GameResponse expectedResponse = mock(GameResponse.class);

        when(playerRepository.findByName("John")).thenReturn(Mono.just(player));
        when(deckManager.generateShuffledDeck()).thenReturn(new ArrayList<>(deck));
        when(gameFactory.createNewGame(anyString(), anyList(), anyList(), anyList())).thenReturn(mockGame);
        when(gameRepository.save(any(Games.class))).thenReturn(Mono.just(mockGame));
        when(blackjackEngine.calculateScore(anyList())).thenReturn(10);
        when(gameMapper.toResponse(any(Games.class), anyList(), anyList())).thenReturn(expectedResponse);

        // Act & Assert
        StepVerifier.create(gameCreationService.createGame("John"))
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(playerRepository).findByName("John");
        verify(deckManager).generateShuffledDeck();
        verify(gameFactory).createNewGame(eq(player.getId()), anyList(), anyList(), anyList());
        verify(gameRepository).save(mockGame);
        verify(gameMapper).toResponse(mockGame, playerCards, dealerCards);
    }
}

