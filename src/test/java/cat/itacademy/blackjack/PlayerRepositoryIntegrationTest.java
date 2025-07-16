package cat.itacademy.blackjack;

import cat.itacademy.blackjack.model.Player;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MongoDBContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
@DataMongoTest
@AutoConfigureDataMongo
@ContextConfiguration(initializers = MongoDbTestContainer.Initializer.class)
public class PlayerRepositoryIntegrationTest {

    @Autowired
    private PlayerRepository playerRepository;

    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    static {
        mongoDBContainer.start();
    }


    @Test
    void testSaveAndFind() {
        Player player = Player.builder().name("Alice").totalScore(0).build();

        Mono<Player> saved = playerRepository.save(player);
        StepVerifier.create(saved).expectNextMatches(p -> p.getName().equals("Alice")).verifyComplete();
    }
}