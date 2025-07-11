package cat.itacademy.blackjack;

import cat.itacademy.blackjack.model.Games;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class GameRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("blackjack")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(3306) + "/blackjack"
        );
        registry.add("spring.r2dbc.username", mysql::getUsername);
        registry.add("spring.r2dbc.password", mysql::getPassword);
        registry.add("spring.flyway.enabled", () -> "false"); // si usas flyway
    }

    @Autowired
    private GameRepository gameRepository;

    @Test
    void testSaveGame() {
        Games game = Games.builder().playerId("mongoId123").playerScore(10).dealerScore(20).build();

        Mono<Games> saved = gameRepository.save(game);
        StepVerifier.create(saved).expectNextMatches(g -> g.getPlayerId().equals("mongoId123")).verifyComplete();
    }
}
