package cat.itacademy.blackjack.service.logic;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.InvalidGameStateException;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.*;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.service.engine.BlackjackEngine;
import cat.itacademy.blackjack.service.engine.DeckManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameStandProcessor {

    private final GameRepository gameRepository;
    private final DeckManager deckManager;
    private final BlackjackEngine blackjackEngine;
    private final GameMapper gameMapper;
    private final PlayerStatsUpdater playerStatsUpdater;

    public Mono<GameResponse> processStand(Long gameId) {
        if (gameId == null) {
            return Mono.error(new GameNotFoundException("Game ID must not be null"));
        }

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .flatMap(game -> {
                    if (game.getTurn() != GameTurn.PLAYER_TURN || game.getStatus() != GameStatus.IN_PROGRESS) {
                        return Mono.error(new InvalidGameStateException("Game is not in player's turn"));
                    }

                    return Mono.zip(
                            deckManager.deserializeCardsReactive(game.getDealerCardsJson()),
                            deckManager.deserializeCardsReactive(game.getPlayerCardsJson()),
                            deckManager.deserializeCardsReactive(game.getDeckJson())
                    ).flatMap(tuple -> {
                        List<Card> dealerInitialCards = tuple.getT1();
                        List<Card> playerCards = tuple.getT2();
                        List<Card> deck = tuple.getT3();

                        // Simular turno del dealer
                        TurnResult dealerTurn = blackjackEngine.simulateTurnWithInitial(dealerInitialCards, deck);
                        int dealerScore = dealerTurn.score();
                        int playerScore = blackjackEngine.calculateScore(playerCards);

                        GameStatus finalStatus = blackjackEngine.determineWinner(playerScore, dealerScore);

                        // Actualizar juego
                        game.setDealerCards(dealerTurn.cards());
                        game.setDealerScore(dealerScore);
                        game.setDealerCardsJson(deckManager.serializeCards(dealerTurn.cards()));
                        game.setDeckJson(deckManager.serializeCards(deck));
                        game.setStatus(finalStatus);
                        game.setTurn(GameTurn.FINISHED);

                        return gameRepository.save(game)
                                .flatMap(updated ->
                                        playerStatsUpdater.updateAfterGameIfFinished(updated)
                                                .thenReturn(gameMapper.toResponse(updated, playerCards, dealerTurn.cards()))
                                );
                    });
                });
    }

}

