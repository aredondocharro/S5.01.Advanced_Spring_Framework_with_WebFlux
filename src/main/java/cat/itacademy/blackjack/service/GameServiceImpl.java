package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.*;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
public class GameServiceImpl implements GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final GameMapper gameMapper;
    private final DeckService deckService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                    List<Card> deck = generateDeck();
                    String deckJson;
                    try {
                        deckJson = objectMapper.writeValueAsString(deck);
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to serialize deck for new game", e);
                        return Mono.error(new RuntimeException("Failed to create game due to deck serialization error."));
                    }

                    Games game = Games.builder()
                            .playerId(player.getId())
                            .createdAt(LocalDateTime.now())
                            .status(GameStatus.IN_PROGRESS)
                            .playerScore(0)
                            .dealerScore(0)
                            .deckJson(deckJson) // ← Aquí se asigna
                            .build();

                    logger.debug("New game entity prepared: {}", game);
                    return gameRepository.save(game);
                })
                .doOnSuccess(savedGame -> logger.info("Game created with ID: {}", savedGame.getId()))
                .map(gameMapper::toResponse);
    }

    private List<Card> generateDeck() {
        List<Card> deck = new ArrayList<>();
        for (CardSuit suit : CardSuit.values()) {
            for (CardValue value : CardValue.values()) {
                deck.add(Card.builder().suit(suit).value(value).build());
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    private Mono<List<Card>> parseDeckReactive(String deckJson) {
        return Mono.fromCallable(() ->
                        objectMapper.readValue(deckJson, new TypeReference<List<Card>>() {})
                ).subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> logger.error("Failed to deserialize deckJson", e));
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
                .doOnSuccess(game -> logger.debug("Game retrieved: {}", game))
                .map(gameMapper::toResponse);
    }

    @Override
    public Flux<GameResponse> getAllGames() {
        logger.info("Retrieving all games from repository");
        return gameRepository.findAll()
                .map(gameMapper::toResponse)
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
                .flatMap(game -> {
                    logger.debug("Game found for deletion: {}", game);
                    return gameRepository.delete(game)
                            .doOnSuccess(v -> logger.info("Game deleted: {}", gameId));
                });
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
                        logger.info("Game ID {} is already finished. Returning current status.", gameId);
                        return Mono.just(gameMapper.toResponse(game));
                    }

                    return parseDeckReactive(game.getDeckJson())
                            .flatMap(deck -> {
                                TurnResult playerTurn = simulateTurn(deck);
                                TurnResult dealerTurn = simulateTurn(deck);

                                int playerScore = playerTurn.score();
                                int dealerScore = dealerTurn.score();

                                logger.debug("Player score: {}, Dealer score: {}", playerScore, dealerScore);

                                GameStatus result;
                                if (playerScore > 21) {
                                    result = GameStatus.DEALER_WON;
                                } else if (dealerScore > 21) {
                                    result = GameStatus.PLAYER_WON;
                                } else if (playerScore > dealerScore) {
                                    result = GameStatus.PLAYER_WON;
                                } else if (dealerScore > playerScore) {
                                    result = GameStatus.DEALER_WON;
                                } else {
                                    result = GameStatus.DRAW;
                                }

                                String updatedDeckJson;
                                try {
                                    updatedDeckJson = objectMapper.writeValueAsString(deck); // mazo actualizado
                                } catch (JsonProcessingException e) {
                                    logger.error("Failed to serialize updated deck for game ID: {}", gameId, e);
                                    return Mono.error(new RuntimeException("Failed to update game due to deck serialization error."));
                                }

                                game.setPlayerScore(playerScore);
                                game.setDealerScore(dealerScore);
                                game.setStatus(result);
                                game.setDeckJson(updatedDeckJson);

                                return gameRepository.save(game)
                                        .doOnSuccess(updated -> logger.info("Game ID {} updated with result: {}", gameId, result))
                                        .map(updatedGame -> new GameResponse(
                                                updatedGame.getId(),
                                                updatedGame.getPlayerId(),
                                                updatedGame.getCreatedAt(),
                                                updatedGame.getStatus(),
                                                updatedGame.getPlayerScore(),
                                                updatedGame.getDealerScore(),
                                                playerTurn.cards(),
                                                dealerTurn.cards()
                                        ));
                            });
                });
    }

    private TurnResult simulateTurn(List<Card> deck) {
        List<Card> cards = new ArrayList<>();
        int score = 0;

        while (score < 17 && !deck.isEmpty()) {
            Card card = deck.remove(0);
            cards.add(card);
            score = calculateScore(cards);
        }

        logger.debug("Turn simulated. Cards: {}, Score: {}", cards, score);
        return new TurnResult(score, cards);
    }

    private int calculateScore(List<Card> cards) {
        return cards.stream()
                .mapToInt(card -> card.getValue().getPoints())
                .sum();
    }
}


