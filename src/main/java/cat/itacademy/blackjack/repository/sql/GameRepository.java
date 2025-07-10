package cat.itacademy.blackjack.repository.sql;

import cat.itacademy.blackjack.model.Games;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface GameRepository extends ReactiveCrudRepository<Games, Long> {

    Mono<Games> findByPlayerId(String playerId);
}