package cat.itacademy.blackjack.gameservice;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.InvalidGameStateException;
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
    private GameMapper gameMapper;

    @Mock
    private DeckManager deckManager;

    @Mock
    private BlackjackEngine blackjackEngine;

    @InjectMocks
    private GameStandProcessor gameStandProcessor;

    private Games game;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        game = new Games();
        game.setId(1L);
        game.setTurn(GameTurn.PLAYER_TURN);
        game.setPlayerId("player123");
        game.setStatus(GameStatus.IN_PROGRESS);

        List<Card> playerCards = List.of(
                new Card(CardSuit.HEARTS, CardValue.EIGHT),
                new Card(CardSuit.SPADES, CardValue.SEVEN)
        );
        game.setPlayerCards(playerCards);
        game.setPlayerCardsJson("[{\"suit\":\"HEARTS\",\"value\":\"EIGHT\"},{\"suit\":\"SPADES\",\"value\":\"SEVEN\"}]");

        List<Card> dealerCards = List.of(
                new Card(CardSuit.DIAMONDS, CardValue.TWO),
                new Card(CardSuit.CLUBS, CardValue.THREE)
        );
        game.setDealerCards(dealerCards);
        game.setDealerCardsJson("[{\"suit\":\"DIAMONDS\",\"value\":\"TWO\"},{\"suit\":\"CLUBS\",\"value\":\"THREE\"}]");

        game.setDeckJson("[{\"suit\":\"DIAMONDS\",\"value\":\"TEN\"},{\"suit\":\"SPADES\",\"value\":\"FIVE\"}]");

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
                .expectError(InvalidGameStateException.class)
                .verify();
    }

    @Test
    void processStand_shouldSimulateDealerTurn_andReturnUpdatedGame() {
        List<Card> deck = List.of(
                new Card(CardSuit.DIAMONDS, CardValue.TEN),
                new Card(CardSuit.SPADES, CardValue.FIVE)
        );
        List<Card> playerCards = game.getPlayerCards();
        List<Card> dealerCards = game.getDealerCards();


        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));
        when(deckManager.deserializeCardsReactive(game.getDealerCardsJson())).thenReturn(Mono.just(dealerCards));
        when(deckManager.deserializeCardsReactive(game.getPlayerCardsJson())).thenReturn(Mono.just(playerCards));
        when(deckManager.deserializeCardsReactive(game.getDeckJson())).thenReturn(Mono.just(deck));

        when(blackjackEngine.calculateScore(playerCards)).thenReturn(15);
        when(blackjackEngine.calculateScore(dealerCards)).thenReturn(16).thenReturn(18); // simula que pide una carta
        when(blackjackEngine.determineWinner(15, 18)).thenReturn(GameStatus.FINISHED_DEALER_WON);

        when(deckManager.serializeCards(anyList())).thenReturn("serializedDeck");
        when(gameRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(gameMapper.toResponse(any(), anyList(), anyList())).thenReturn(mock(GameResponse.class));

        StepVerifier.create(gameStandProcessor.processStand(1L))
                .expectNextCount(1)
                .verifyComplete();
    }
}
