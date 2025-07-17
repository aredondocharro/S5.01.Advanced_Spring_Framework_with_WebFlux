package cat.itacademy.blackjack.service.logic;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.InsufficientCardsException;
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

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GameHitProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GameHitProcessor.class);

    private final GameRepository gameRepository;
    private final DeckManager deckManager;
    private final BlackjackEngine blackjackEngine;
    private final GameMapper gameMapper;
    private final PlayerStatsUpdater playerStatsUpdater;

    public Mono<GameResponse> processHit(Long gameId) {
        if (gameId == null) {
            logger.warn("Attempted to process hit with null gameId");
            return Mono.error(new GameNotFoundException("Game ID must not be null"));
        }

        logger.debug("Starting hit process for game ID: {}", gameId);

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("Game with ID {} not found", gameId);
                    return Mono.error(new GameNotFoundException(gameId));
                }))
                .flatMap(game -> {
                    if (game.getTurn() != GameTurn.PLAYER_TURN || game.getStatus() != GameStatus.IN_PROGRESS) {
                        logger.warn("Invalid game state for hit. Game ID: {}, Turn: {}, Status: {}",
                                game.getId(), game.getTurn(), game.getStatus());
                        return Mono.error(new InvalidGameStateException("Game is already finished or not in player's turn."));
                    }

                    logger.debug("Deserializing deck and cards for game ID: {}", gameId);

                    return Mono.zip(
                            deckManager.deserializeCardsReactive(game.getDeckJson()),
                            deckManager.deserializeCardsReactive(game.getPlayerCardsJson()),
                            deckManager.deserializeCardsReactive(game.getDealerCardsJson())
                    ).flatMap(tuple -> {
                        List<Card> deck = new ArrayList<>(tuple.getT1());
                        List<Card> playerCards = new ArrayList<>(tuple.getT2());
                        List<Card> dealerCards = tuple.getT3();

                        if (deck.isEmpty()) {
                            logger.warn("Deck is empty for game ID: {}", gameId);
                            return Mono.error(new InsufficientCardsException("No cards left in deck"));
                        }

                        Card newCard = deck.remove(0);
                        playerCards.add(newCard);
                        int playerScore = blackjackEngine.calculateScore(playerCards);

                        logger.info("Player hit in game {}: drew {}, new score {}", gameId, newCard, playerScore);

                        game.setPlayerCardsJson(deckManager.serializeDeck(playerCards));
                        game.setDeckJson(deckManager.serializeDeck(deck));
                        game.setPlayerScore(playerScore);

                        if (playerScore > 21) {
                            game.setStatus(GameStatus.FINISHED_DEALER_WON);
                            game.setTurn(GameTurn.FINISHED);
                            logger.info("Player bust in game {}. Game ends with status: {}", gameId, game.getStatus());
                        } else if (playerScore == 21) {
                            GameStatus resolved = blackjackEngine.determineWinner(playerScore, game.getDealerScore());
                            game.setStatus(resolved);
                            game.setTurn(GameTurn.FINISHED);
                            logger.info("Player hit 21 in game {}. Game ends with status: {}", gameId, resolved);
                        } else {
                            logger.debug("Player continues after hit. Score: {}", playerScore);
                        }

                        return gameRepository.save(game)
                                .doOnNext(saved -> logger.debug("Game {} saved after hit. Current status: {}", saved.getId(), saved.getStatus()))
                                .flatMap(updated ->
                                        playerStatsUpdater.updateAfterGameIfFinished(updated)
                                                .thenReturn(gameMapper.toResponse(updated, playerCards, dealerCards))
                                );
                    });
                });
    }
}



