package cat.itacademy.blackjack.gameservice;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.*;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.service.engine.BlackjackEngine;
import cat.itacademy.blackjack.service.engine.DeckManager;
import cat.itacademy.blackjack.service.logic.GameStandProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GameStandProcessorTest {

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

    @InjectMocks
    private GameStandProcessor gameStandProcessor;

    private Games game;
    private Player player;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        game = new Games();
        game.setId(1L);
        game.setPlayerId("player123");
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setDeckJson("[]");
        game.setPlayerCards(List.of(new Card(CardSuit.HEARTS, CardValue.EIGHT), new Card(CardSuit.SPADES, CardValue.SEVEN)));
        player = new Player("player123", "John", 0, 0, 0, null);
    }

    @Test
    void processStand_shouldFail_whenGameIdIsNull() {
        StepVerifier.create(gameStandProcessor.processStand(null))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void processStand_shouldFail_whenGameNotFound() {
        when(gameRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(gameStandProcessor.processStand(1L))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void processStand_shouldFail_whenGameAlreadyFinished() {
        game.setStatus(GameStatus.FINISHED_PLAYER_WON);
        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));

        StepVerifier.create(gameStandProcessor.processStand(1L))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void processStand_shouldSimulateDealerTurn_andReturnUpdatedGame() {
        List<Card> deck = List.of(
                new Card(CardSuit.DIAMONDS, CardValue.TEN),
                new Card(CardSuit.CLUBS, CardValue.FIVE)
        );
        TurnResult dealerTurn = new TurnResult(18, deck);

        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));
        when(deckManager.deserializeCardsReactive(anyString())).thenReturn(Mono.just(deck));
        when(blackjackEngine.simulateTurn(anyList())).thenReturn(dealerTurn);
        when(blackjackEngine.calculateScore(game.getPlayerCards())).thenReturn(15);
        when(blackjackEngine.determineWinner(15, 18)).thenReturn(GameStatus.FINISHED_DEALER_WON);
        when(deckManager.serializeDeck(deck)).thenReturn("serializedDeck");
        when(gameRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(playerRepository.findById("player123")).thenReturn(Mono.just(player));
        when(playerRepository.save(any())).thenReturn(Mono.just(player));
        when(gameMapper.toResponse(any(), anyList(), anyList())).thenReturn(mock(GameResponse.class));

        StepVerifier.create(gameStandProcessor.processStand(1L))
                .expectNextCount(1)
                .verifyComplete();
    }
}
