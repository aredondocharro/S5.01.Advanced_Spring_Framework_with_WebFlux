package cat.itacademy.blackjack.repository;

import cat.itacademy.blackjack.model.Player;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface PlayerRepository extends ReactiveMongoRepository<Player, String> {

    Mono<Player> findByName(String name);
}