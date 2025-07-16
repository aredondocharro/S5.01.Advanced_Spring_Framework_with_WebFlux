package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.InsufficientCardsException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.CardMapper;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;

@RequiredArgsConstructor
@Service
public class GameServiceImpl implements GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final GameMapper gameMapper;
    private final DeckManager deckManager;
    private final GameFactory gameFactory;
    private final BlackjackEngine blackjackEngine;

    @Override
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
                        return Mono.error(new InsufficientCardsException("Not enough cards in the deck to start a game"));
                    }

                    List<Card> playerCards = List.of(deck.remove(0), deck.remove(0));
                    List<Card> dealerCards = List.of(deck.remove(0), deck.remove(0));

                    Games game = gameFactory.createNewGame(player.getId(), playerCards, dealerCards, deck);
                    if (game == null) {
                        return Mono.error(new RuntimeException("Game creation failed"));
                    }

                    return gameRepository.save(game)
                            .map(savedGame -> {
                                savedGame.setPlayerCards(playerCards);
                                savedGame.setDealerCards(dealerCards);
                                return gameMapper.toResponse(savedGame, playerCards, dealerCards);
                            });
                });
    }

    @Override
    public Mono<GameResponse> getGameById(Long gameId) {
        if (gameId == null) {
            logger.warn("Attempt to get game with null ID");
            return Mono.error(new GameNotFoundException("Game ID must not be null."));
        }

        logger.info("Fetching game with ID: {}", gameId);

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .flatMap(game -> {
                    Mono<Tuple2<List<Card>, List<Card>>> splitDeckMono =
                            deckManager.parseDeckReactive(game.getDeckJson())
                                    .map(deckManager::splitDeck);

                    return splitDeckMono.map(tuple -> gameMapper.toResponse(game, tuple.getT1(), tuple.getT2()));
                });
    }


    @Override
    public Flux<GameResponse> getAllGames() {
        logger.info("Retrieving all games from repository");

        return gameRepository.findAll()
                .flatMap(game -> deckManager.parseDeckReactive(game.getDeckJson())
                        .map(deckManager::splitDeck)
                        .map(tuple -> gameMapper.toResponse(game, tuple.getT1(), tuple.getT2())))
                .doOnComplete(() -> logger.info("Completed fetching all games"));
    }

    @Override
    public Mono<Void> deleteGame(Long gameId) {
        if (gameId == null) {
            logger.warn("Attempt to delete game with null ID");
            return Mono.error(new GameNotFoundException("Game ID must not be null."));
        }

        logger.info("Deleting game with ID: {}", gameId);
        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .flatMap(game -> gameRepository.delete(game)
                        .doOnSuccess(v -> logger.info("Game deleted: {}", gameId)));
    }

    @Override
    public Mono<GameResponse> playGame(Long gameId) {
        if (gameId == null) {
            logger.warn("Attempt to play game with null ID");
            return Mono.error(new GameNotFoundException("Game ID must not be null"));
        }

        logger.info("Starting game round for ID: {}", gameId);

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .flatMap(game -> {
                    if (game.getStatus() != GameStatus.IN_PROGRESS) {
                        return deckManager.parseDeckReactive(game.getDeckJson())
                                .map(deckManager::splitDeck)
                                .map(tuple -> gameMapper.toResponse(game, tuple.getT1(), tuple.getT2()));
                    }

                    return deckManager.parseDeckReactive(game.getDeckJson())
                            .flatMap(deck -> {
                                if (deck.size() < 4) {
                                    return Mono.error(new InsufficientCardsException("Not enough cards left in the deck to continue the game"));
                                }

                                TurnResult playerTurn = blackjackEngine.simulateTurn(deck);
                                TurnResult dealerTurn = blackjackEngine.simulateTurn(deck);

                                GameStatus result = blackjackEngine.determineWinner(playerTurn.score(), dealerTurn.score());

                                String updatedDeckJson = deckManager.serializeDeck(deck);

                                game.setPlayerScore(playerTurn.score());
                                game.setDealerScore(dealerTurn.score());
                                game.setStatus(result);
                                game.setDeckJson(updatedDeckJson);

                                return gameRepository.save(game)
                                        .flatMap(updatedGame ->
                                                playerRepository.findById(updatedGame.getPlayerId())
                                                        .flatMap(player -> {
                                                            player.setGamesPlayed(player.getGamesPlayed() + 1);

                                                            // Sumar puntos y victorias solo si gana el jugador
                                                            if (result == GameStatus.FINISHED_PLAYER_WON) {
                                                                player.setGamesWon(player.getGamesWon() + 1);
                                                                player.setTotalScore(player.getTotalScore() + updatedGame.getPlayerScore());
                                                            }

                                                            return playerRepository.save(player);
                                                        })
                                                        .thenReturn(gameMapper.toResponse(
                                                                updatedGame,
                                                                playerTurn.cards(),
                                                                dealerTurn.cards()
                                                        ))
                                        );
                            });
                });
    }

}


