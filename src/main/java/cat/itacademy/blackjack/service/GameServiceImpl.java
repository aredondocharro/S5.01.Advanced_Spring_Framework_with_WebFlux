package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.model.Games;
import cat.itacademy.blackjack.model.GameStatus;
import cat.itacademy.blackjack.model.Player;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;

    @Override
    public Mono<Games> createGame(String playerName) {
        return playerRepository.findByName(playerName)
                .switchIfEmpty(playerRepository.save(Player.builder()
                        .name(playerName)
                        .gamesPlayed(0)
                        .gamesWon(0)
                        .build()))
                .flatMap(player -> {
                    Games game = Games.builder()
                            .playerId(player.getId())
                            .createdAt(LocalDateTime.now())
                            .status(GameStatus.IN_PROGRESS)
                            .playerScore(0)
                            .dealerScore(0)
                            .build();
                    return gameRepository.save(game);
                });
    }
}