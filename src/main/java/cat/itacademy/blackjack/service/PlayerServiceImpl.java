package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.PlayerRankingResponse;
import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.exception.InvalidPlayerNameException;
import cat.itacademy.blackjack.exception.PlayerAlreadyExistsException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.PlayerMapper;
import cat.itacademy.blackjack.model.Player;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerServiceImpl.class);

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    @Override
    public Mono<PlayerResponse> create(PlayerRequest request) {
        String name = request.name();
        if (name == null || name.trim().isEmpty()) {
            return Mono.error(new InvalidPlayerNameException("Player name cannot be null or empty"));
        }

        return playerRepository.findByName(name)
                .flatMap(existing -> Mono.<PlayerResponse>error(new PlayerAlreadyExistsException("Player with that name already exists.")))
                .switchIfEmpty(Mono.defer(() -> {
                    Player player = playerMapper.toEntity(request);
                    return playerRepository.save(player)
                            .map(playerMapper::toResponse);
                }));
    }


    @Override
    public Mono<PlayerResponse> findByName(String name) {
        logger.debug("Finding player by name: {}", name);
        return playerRepository.findByName(name)
                .switchIfEmpty(Mono.error(PlayerNotFoundException.forMissingName(name)))
                .doOnSuccess(p -> logger.info("Player found: {}", p.getName()))
                .map(playerMapper::toResponse);
    }

    @Override
    public Mono<PlayerResponse> findById(String id) {
        logger.debug("Finding player by ID: {}", id);
        return playerRepository.findById(id)
                .switchIfEmpty(Mono.error(PlayerNotFoundException.forMissingId(id)))
                .doOnSuccess(p -> logger.info("Player found by ID: {}", id))
                .map(playerMapper::toResponse);
    }

    @Override
    public Flux<PlayerResponse> findAll() {
        logger.info("Retrieving all players");
        return playerRepository.findAll()
                .doOnComplete(() -> logger.info("All players retrieved"))
                .map(playerMapper::toResponse);
    }

    @Override
    public Flux<PlayerRankingResponse> getRanking() {
        logger.info("Retrieving player ranking by win rate and total score");

        return playerRepository.findAll()
                .map(player -> {
                    double winRate = player.getGamesPlayed() == 0 ? 0.0 :
                            (double) player.getGamesWon() / player.getGamesPlayed();

                    return new PlayerRankingResponse(
                            player.getName(),
                            player.getGamesPlayed(),
                            player.getGamesWon(),
                            winRate,
                            player.getTotalScore()
                    );
                })
                .sort((p1, p2) -> {
                    int byWinRate = Double.compare(p2.winRate(), p1.winRate());
                    if (byWinRate != 0) return byWinRate;
                    return Integer.compare(p2.totalScore(), p1.totalScore());
                })
                .doOnComplete(() -> logger.info("Ranking retrieval completed"));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            logger.warn("Attempted to delete player with null or empty ID");
            return Mono.error(new PlayerNotFoundException("Player ID must not be null or empty."));
        }

        logger.warn("Deleting player with ID: {}", id);
        return playerRepository.findById(id)
                .switchIfEmpty(Mono.error(new PlayerNotFoundException("Player with id '" + id + "' not found.")))
                .flatMap(playerRepository::delete)
                .doOnSuccess(unused -> logger.info("Player deleted with ID: {}", id));
    }
}


