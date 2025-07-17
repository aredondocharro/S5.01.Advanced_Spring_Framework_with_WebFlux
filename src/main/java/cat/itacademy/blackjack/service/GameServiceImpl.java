package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
                        logger.error("Not enough cards to start a game");
                        return Mono.error(new InsufficientCardsException("Not enough cards in the deck to start a game"));
                    }

                    List<Card> playerCards = List.of(deck.remove(0), deck.remove(0));
                    List<Card> dealerCards = List.of(deck.remove(0), deck.remove(0));

                    Games game = gameFactory.createNewGame(player.getId(), playerCards, dealerCards, deck);
                    if (game == null) {
                        logger.error("Game creation failed");
                        return Mono.error(new RuntimeException("Game creation failed"));
                    }

                    game.setTurn(GameTurn.PLAYER_TURN);
                    game.setPlayerScore(blackjackEngine.calculateScore(playerCards));
                    game.setDealerScore(0); // se calcula en stand()
                    game.setStatus(GameStatus.IN_PROGRESS);
                    game.setCreatedAt(LocalDateTime.now());

                    return gameRepository.save(game)
                            .doOnSuccess(saved -> logger.info("Game created with ID: {}", saved.getId()))
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
    public Mono<GameResponse> hit(Long gameId) {
        if (gameId == null) {
            return Mono.error(new GameNotFoundException("Game ID must not be null"));
        }

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .flatMap(game -> {
                    if (game.getTurn() != GameTurn.PLAYER_TURN || game.getStatus() != GameStatus.IN_PROGRESS) {
                        return Mono.error(new IllegalStateException("Game is not in player's turn"));
                    }

                    return deckManager.parseDeckReactive(game.getDeckJson())
                            .flatMap(deck -> {
                                if (deck.isEmpty()) {
                                    return Mono.error(new InsufficientCardsException("No cards left in deck"));
                                }

                                Card newCard = deck.remove(0);
                                List<Card> playerCards = game.getPlayerCards() != null
                                        ? new ArrayList<>(game.getPlayerCards())
                                        : new ArrayList<>();
                                playerCards.add(newCard);

                                int newScore = blackjackEngine.calculateScore(playerCards);

                                game.setPlayerCards(playerCards);
                                game.setPlayerScore(newScore);
                                game.setDeckJson(deckManager.serializeDeck(deck));

                                if (newScore > 21) {
                                    game.setStatus(GameStatus.FINISHED_DEALER_WON);
                                    game.setTurn(GameTurn.FINISHED);
                                }

                                return gameRepository.save(game)
                                        .map(updated -> gameMapper.toResponse(updated, playerCards, game.getDealerCards()));
                            });
                });
    }
    @Override
    public Mono<GameResponse> stand(Long gameId) {
        if (gameId == null) {
            logger.warn("Attempt to stand with null game ID");
            return Mono.error(new GameNotFoundException("Game ID must not be null"));
        }

        logger.info("Player stands in game ID: {}", gameId);

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .flatMap(game -> {
                    if (game.getStatus() != GameStatus.IN_PROGRESS) {
                        return Mono.error(new IllegalStateException("Game is already finished"));
                    }

                    return deckManager.parseDeckReactive(game.getDeckJson())
                            .flatMap(deck -> {
                                List<Card> playerCards = game.getPlayerCards(); // ya están en memoria
                                TurnResult dealerTurn = blackjackEngine.simulateTurn(deck);

                                int playerScore = blackjackEngine.calculateScore(playerCards);
                                int dealerScore = dealerTurn.score();
                                GameStatus result = blackjackEngine.determineWinner(playerScore, dealerScore);
                                String updatedDeckJson = deckManager.serializeDeck(deck);

                                // Actualizamos la partida
                                game.setDealerCards(dealerTurn.cards());
                                game.setDealerScore(dealerScore);
                                game.setPlayerScore(playerScore);
                                game.setDeckJson(updatedDeckJson);
                                game.setStatus(result);

                                return gameRepository.save(game)
                                        .flatMap(updatedGame ->
                                                playerRepository.findById(game.getPlayerId())
                                                        .flatMap(player -> {
                                                            player.setGamesPlayed(player.getGamesPlayed() + 1);

                                                            if (result == GameStatus.FINISHED_PLAYER_WON) {
                                                                player.setGamesWon(player.getGamesWon() + 1);
                                                                player.setTotalScore(player.getTotalScore() + playerScore);
                                                            }

                                                            return playerRepository.save(player)
                                                                    .thenReturn(gameMapper.toResponse(
                                                                            updatedGame,
                                                                            playerCards,
                                                                            dealerTurn.cards()
                                                                    ));
                                                        }));
                            });
                });
    }
/*    @Override
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
    }*/

}


