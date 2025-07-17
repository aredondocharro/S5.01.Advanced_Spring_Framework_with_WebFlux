package cat.itacademy.blackjack;

import cat.itacademy.blackjack.model.GameStatus;
import cat.itacademy.blackjack.model.GameTurn;
import cat.itacademy.blackjack.model.Games;
import cat.itacademy.blackjack.repository.sql.GameRepository;
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

import java.time.LocalDateTime;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class GameRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("blackjack")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("init.sql");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(3306) + "/blackjack"
        );
        registry.add("spring.r2dbc.username", mysql::getUsername);
        registry.add("spring.r2dbc.password", mysql::getPassword);
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private GameRepository gameRepository;

    @Test
    void testSaveGame() {
        Games game = Games.builder()
                .playerId("mongoId123")
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .turn(GameTurn.PLAYER_TURN)
                .playerScore(10)
                .dealerScore(20)
                .deckJson("[]")
                .playerCardsJson("[]")
                .dealerCardsJson("[]")
                .build();

        Mono<Games> saved = gameRepository.save(game);

        StepVerifier.create(saved)
                .expectNextMatches(g -> g.getPlayerId().equals("mongoId123"))
                .verifyComplete();
    }
}
