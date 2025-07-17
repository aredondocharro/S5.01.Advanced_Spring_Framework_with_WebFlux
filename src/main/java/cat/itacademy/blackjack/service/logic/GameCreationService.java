package cat.itacademy.blackjack.service.logic;

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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameCreationService {

    private static final Logger logger = LoggerFactory.getLogger(GameCreationService.class);

    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;
    private final DeckManager deckManager;
    private final GameFactory gameFactory;
    private final GameMapper gameMapper;
    private final PlayerStatsUpdater playerStatsUpdater;

    public Mono<GameResponse> createGame(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            logger.warn("Attempt to create game with null or empty player name.");
            return Mono.error(PlayerNotFoundException.forInvalidInput());
        }

        logger.info("Creating game for player: {}", playerName);

        return playerRepository.findByName(playerName)
                .switchIfEmpty(Mono.error(PlayerNotFoundException.forMissingName(playerName)))
                .flatMap(player -> {
                    List<Card> deck = deckManager.generateShuffledDeck();

                    if (deck.size() < 4) {
                        logger.error("Not enough cards to start a game");
                        return Mono.error(new InsufficientCardsException("Not enough cards in the deck to start a game"));
                    }

                    List<Card> playerCards = List.of(deck.remove(0), deck.remove(0));
                    List<Card> dealerCards = List.of(deck.remove(0), deck.remove(0));

                    Games game = gameFactory.createNewGame(player.getId(), playerCards, dealerCards, deck);
                    game.setTurn(GameTurn.PLAYER_TURN);
                    game.setPlayerCards(playerCards);
                    game.setDealerCards(dealerCards);

                    return gameRepository.save(game)
                            .doOnSuccess(saved -> logger.info("Game created with ID: {}", saved.getId()))
                            .flatMap(savedGame ->
                                    playerStatsUpdater.updateAfterGameIfFinished(savedGame)
                                            .thenReturn(gameMapper.toResponse(savedGame, playerCards, dealerCards))
                            );
                });
    }
}
