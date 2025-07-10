package cat.itacademy.blackjack.repository.mongo;

import cat.itacademy.blackjack.model.Player;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface PlayerRepository extends ReactiveMongoRepository<Player, String> {

    Mono<Player> findByName(String name);
}