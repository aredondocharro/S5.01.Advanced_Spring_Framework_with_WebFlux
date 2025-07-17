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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameStandProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GameStandProcessor.class);

    private final GameRepository gameRepository;
    private final DeckManager deckManager;
    private final BlackjackEngine blackjackEngine;
    private final GameMapper gameMapper;
    private final PlayerStatsUpdater playerStatsUpdater;

    public Mono<GameResponse> processStand(Long gameId) {
        if (gameId == null) {
            logger.warn("Attempted to process stand with null gameId");
            return Mono.error(new GameNotFoundException("Game ID must not be null"));
        }

        logger.debug("Starting stand process for game ID: {}", gameId);

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("Game with ID {} not found", gameId);
                    return Mono.error(new GameNotFoundException(gameId));
                }))
                .flatMap(game -> {
                    if (game.getTurn() != GameTurn.PLAYER_TURN || game.getStatus() != GameStatus.IN_PROGRESS) {
                        logger.warn("Invalid game state for stand. Game ID: {}, Turn: {}, Status: {}",
                                game.getId(), game.getTurn(), game.getStatus());
                        return Mono.error(new InvalidGameStateException("Game is not in player's turn"));
                    }

                    logger.debug("Deserializing cards for game ID: {}", gameId);
                    return Mono.zip(
                            deckManager.deserializeCardsReactive(game.getDealerCardsJson()),
                            deckManager.deserializeCardsReactive(game.getPlayerCardsJson()),
                            deckManager.deserializeCardsReactive(game.getDeckJson())
                    ).flatMap(tuple -> {
                        List<Card> dealerInitialCards = tuple.getT1();
                        List<Card> playerCards = tuple.getT2();
                        List<Card> deck = tuple.getT3();

                        logger.debug("Simulating dealer's turn. Dealer initial cards: {}", dealerInitialCards);
                        TurnResult dealerTurn = blackjackEngine.simulateTurnWithInitial(dealerInitialCards, deck);
                        int dealerScore = dealerTurn.score();
                        int playerScore = blackjackEngine.calculateScore(playerCards);

                        GameStatus finalStatus = blackjackEngine.determineWinner(playerScore, dealerScore);

                        logger.info("Game {} resolved. Player score: {}, Dealer score: {}, Final status: {}",
                                gameId, playerScore, dealerScore, finalStatus);

                        game.setDealerCards(dealerTurn.cards());
                        game.setDealerScore(dealerScore);
                        game.setDealerCardsJson(deckManager.serializeCards(dealerTurn.cards()));
                        game.setDeckJson(deckManager.serializeCards(deck));
                        game.setStatus(finalStatus);
                        game.setTurn(GameTurn.FINISHED);

                        return gameRepository.save(game)
                                .doOnNext(saved -> logger.debug("Game {} saved after stand with status {}", saved.getId(), saved.getStatus()))
                                .flatMap(updated ->
                                        playerStatsUpdater.updateAfterGameIfFinished(updated)
                                                .thenReturn(gameMapper.toResponse(updated, playerCards, dealerTurn.cards()))
                                );
                    });
                });
    }

}

