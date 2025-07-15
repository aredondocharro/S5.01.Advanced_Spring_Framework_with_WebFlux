package cat.itacademy.blackjack;

import cat.itacademy.blackjack.dto.GameRequest;
import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.model.GameStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GameControllerIT {

    @Container
    static MongoDBContainer mongo =
            new MongoDBContainer("mongo:6.0.7");

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("blackjack")
                    .withUsername("sa")
                    .withPassword("sa");

    /** Inyecta en el contexto Spring las URLs que usan los repositorios */
    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        // Mongo Reactive
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        // R2DBC Postgres
        String r2dbcUrl = String.format(
                "r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(),
                postgres.getFirstMappedPort(),
                postgres.getDatabaseName()
        );
        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private WebTestClient client;

    @Test
    void fullGameFlow_viaApi_playerWinsOrDraws() {
        // 1) Crear partida
        GameRequest createReq = new GameRequest("Alice");
        GameResponse initial = client.post()
                .uri("/game/new")
                .contentType(APPLICATION_JSON)
                .bodyValue(createReq)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GameResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(initial);
        assertEquals(GameStatus.IN_PROGRESS, initial.status());
        Long gameId = initial.id();

        // 2) Simular turno del crupier
        GameResponse afterPlay = client.post()
                .uri("/game/{id}/play", gameId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GameResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(afterPlay);
        assertTrue(
                afterPlay.status() == GameStatus.PLAYER_WON ||
                        afterPlay.status() == GameStatus.DEALER_WON  ||
                        afterPlay.status() == GameStatus.FINISHED    ||
                        afterPlay.status() == GameStatus.DRAW
        );
    }
}


