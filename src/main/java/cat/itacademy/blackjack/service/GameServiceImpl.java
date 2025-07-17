package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.service.engine.DeckManager;
import cat.itacademy.blackjack.service.logic.GameCreationService;
import cat.itacademy.blackjack.service.logic.GameHitProcessor;
import cat.itacademy.blackjack.service.logic.GameStandProcessor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class GameServiceImpl implements GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final GameMapper gameMapper;
    private final DeckManager deckManager;

    private final GameCreationService gameCreationService;
    private final GameHitProcessor gameHitProcessor;
    private final GameStandProcessor gameStandProcessor;

    @Override
    public Mono<GameResponse> createGame(String playerName) {
        return gameCreationService.createGame(playerName);
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
                .flatMap(game -> Mono.zip(
                        deckManager.deserializeCardsReactive(game.getPlayerCardsJson()),
                        deckManager.deserializeCardsReactive(game.getDealerCardsJson())
                ).map(tuple -> gameMapper.toResponse(game, tuple.getT1(), tuple.getT2())));
    }

    @Override
    public Flux<GameResponse> getAllGames() {
        logger.info("Retrieving all games from repository");

        return gameRepository.findAll()
                .flatMap(game -> Mono.zip(
                        deckManager.deserializeCardsReactive(game.getPlayerCardsJson()),
                        deckManager.deserializeCardsReactive(game.getDealerCardsJson())
                ).map(tuple -> gameMapper.toResponse(game, tuple.getT1(), tuple.getT2())))
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
        return gameHitProcessor.processHit(gameId);
    }

    @Override
    public Mono<GameResponse> stand(Long gameId) {
        return gameStandProcessor.processStand(gameId);
    }
}
