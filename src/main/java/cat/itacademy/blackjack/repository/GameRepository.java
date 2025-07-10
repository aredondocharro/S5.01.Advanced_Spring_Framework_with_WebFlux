package cat.itacademy.blackjack.repository;

import cat.itacademy.blackjack.model.Game;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface GameRepository extends ReactiveCrudRepository<Game, Long> {

    Flux<Game> findByPlayerId(String playerId); // útil para historial o ranking
}