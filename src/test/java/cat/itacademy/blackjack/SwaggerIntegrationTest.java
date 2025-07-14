package cat.itacademy.blackjack;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SwaggerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void swaggerUiShouldRedirectToIndex() {
        webTestClient.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus().is3xxRedirection(); // HTTP 302 o similar
    }

    @Test
    void apiDocsShouldBeAvailable() {
        webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(10))
                .build()
                .get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json");
    }
}