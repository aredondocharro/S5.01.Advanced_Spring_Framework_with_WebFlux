package cat.itacademy.blackjack.service.logic;

import cat.itacademy.blackjack.model.GameStatus;
import cat.itacademy.blackjack.model.Games;
import cat.itacademy.blackjack.model.Player;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PlayerStatsUpdater {

    private static final Logger logger = LoggerFactory.getLogger(PlayerStatsUpdater.class);

    private final PlayerRepository playerRepository;

    public Mono<Void> updateAfterGameIfFinished(Games game) {
        if (game.getStatus() == null || game.getStatus().name().startsWith("IN_PROGRESS")) {
            logger.debug("Game {} is still in progress, no player stats updated", game.getId());
            return Mono.empty();
        }

        return playerRepository.findById(game.getPlayerId())
                .flatMap(player -> {
                    logger.info("Updating stats for player {} after game {}", player.getName(), game.getId());

                    player.setGamesPlayed(player.getGamesPlayed() + 1);

                    if (game.getStatus() == GameStatus.FINISHED_PLAYER_WON) {
                        player.setGamesWon(player.getGamesWon() + 1);
                    }

                    player.setTotalScore(player.getTotalScore() + game.getPlayerScore());

                    return playerRepository.save(player).then();
                });
    }
}
