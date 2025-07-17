package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.GameResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GameService {
    Mono<GameResponse> createGame(String playerName);
   Mono<GameResponse> hit(Long gameId);
    Mono<GameResponse> stand(Long gameId);
    Mono<GameResponse> getGameById(Long gameId);
    Flux<GameResponse> getAllGames();
    Mono<Void> deleteGame(Long gameId);
}
