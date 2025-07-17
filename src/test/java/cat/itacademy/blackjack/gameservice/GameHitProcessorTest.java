package cat.itacademy.blackjack.gameservice;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.InsufficientCardsException;
import cat.itacademy.blackjack.exception.InvalidGameStateException;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.*;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.service.engine.BlackjackEngine;
import cat.itacademy.blackjack.service.engine.DeckManager;
import cat.itacademy.blackjack.service.logic.GameHitProcessor;
import cat.itacademy.blackjack.service.logic.PlayerStatsUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

class GameHitProcessorTest {

    @Mock private GameRepository gameRepository;
    @Mock private DeckManager deckManager;
    @Mock private BlackjackEngine blackjackEngine;
    @Mock private GameMapper gameMapper;
    @Mock private PlayerStatsUpdater playerStatsUpdater;

    @InjectMocks
    private GameHitProcessor gameHitProcessor;

    private Games game;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        game = new Games();
        game.setId(1L);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setTurn(GameTurn.PLAYER_TURN);
        game.setDeckJson("serialized");
        game.setPlayerCardsJson("[]"); // Necesario para que no pete
        game.setDealerCardsJson("[]");
    }

    @Test
    void processHit_shouldFail_whenIdIsNull() {
        StepVerifier.create(gameHitProcessor.processHit(null))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void processHit_shouldFail_whenGameNotFound() {
        when(gameRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(gameHitProcessor.processHit(1L))
                .expectError(GameNotFoundException.class)
                .verify();
    }

    @Test
    void processHit_shouldFail_whenNotPlayersTurn() {
        game.setTurn(GameTurn.FINISHED);
        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));

        StepVerifier.create(gameHitProcessor.processHit(1L))
                .expectError(InvalidGameStateException.class)
                .verify();
    }

    @Test
    void processHit_shouldFail_whenDeckIsEmpty() {
        game.setDeckJson("deck");
        game.setPlayerCardsJson("player");
        game.setDealerCardsJson("dealer");

        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));
        when(deckManager.deserializeCardsReactive("deck")).thenReturn(Mono.just(new ArrayList<>()));
        when(deckManager.deserializeCardsReactive("player")).thenReturn(Mono.just(List.of(new Card())));
        when(deckManager.deserializeCardsReactive("dealer")).thenReturn(Mono.just(List.of(new Card())));

        StepVerifier.create(gameHitProcessor.processHit(1L))
                .expectError(InsufficientCardsException.class)
                .verify();
    }

    @Test
    void processHit_shouldAddCard_andContinueGame() {
        game.setDeckJson("deck");
        game.setPlayerCardsJson("player");
        game.setDealerCardsJson("dealer");

        List<Card> deck = new ArrayList<>();
        Card newCard = new Card(CardSuit.HEARTS, CardValue.FIVE);
        deck.add(newCard);

        List<Card> playerCards = new ArrayList<>();
        List<Card> dealerCards = new ArrayList<>();

        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));
        when(deckManager.deserializeCardsReactive("deck")).thenReturn(Mono.just(deck));
        when(deckManager.deserializeCardsReactive("player")).thenReturn(Mono.just(playerCards));
        when(deckManager.deserializeCardsReactive("dealer")).thenReturn(Mono.just(dealerCards));
        when(blackjackEngine.calculateScore(anyList())).thenReturn(16); // No bust
        when(deckManager.serializeDeck(anyList())).thenReturn("updatedDeck");
        when(gameRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(playerStatsUpdater.updateAfterGameIfFinished(any())).thenReturn(Mono.empty()); // <--- AÑADIDO
        when(gameMapper.toResponse(any(), anyList(), anyList())).thenReturn(mock(GameResponse.class));

        StepVerifier.create(gameHitProcessor.processHit(1L))
                .expectNextCount(1)
                .verifyComplete();
    }


    @Test
    void processHit_shouldAddCard_andFinishGameIfBust() {
        game.setDeckJson("deck");
        game.setPlayerCardsJson("player");
        game.setDealerCardsJson("dealer");

        List<Card> deck = new ArrayList<>();
        Card newCard = new Card(CardSuit.SPADES, CardValue.KING);
        deck.add(newCard);

        List<Card> playerCards = new ArrayList<>();
        List<Card> dealerCards = new ArrayList<>();

        when(gameRepository.findById(1L)).thenReturn(Mono.just(game));
        when(deckManager.deserializeCardsReactive("deck")).thenReturn(Mono.just(deck));
        when(deckManager.deserializeCardsReactive("player")).thenReturn(Mono.just(playerCards));
        when(deckManager.deserializeCardsReactive("dealer")).thenReturn(Mono.just(dealerCards));
        when(blackjackEngine.calculateScore(anyList())).thenReturn(25); // BUST
        when(deckManager.serializeDeck(anyList())).thenReturn("deckAfterBust");
        when(gameRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(playerStatsUpdater.updateAfterGameIfFinished(any())).thenReturn(Mono.empty()); // <--- AÑADIDO
        when(gameMapper.toResponse(any(), anyList(), anyList())).thenReturn(mock(GameResponse.class));

        StepVerifier.create(gameHitProcessor.processHit(1L))
                .expectNextCount(1)
                .verifyComplete();
    }


}

