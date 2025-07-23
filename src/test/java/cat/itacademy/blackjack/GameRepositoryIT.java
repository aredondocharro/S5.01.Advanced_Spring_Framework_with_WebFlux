package cat.itacademy.blackjack;

import cat.itacademy.blackjack.model.GameStatus;
import cat.itacademy.blackjack.model.GameTurn;
import cat.itacademy.blackjack.model.Games;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

@SpringBootTest
public class GameRepositoryIT extends IntegrationTestBase {

    @Autowired
    private GameRepository gameRepository;

    @Test
    void shouldSaveAndFindGame() {
        Games game = Games.builder()
                .playerId("integrationTestPlayer")
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .turn(GameTurn.PLAYER_TURN)
                .playerScore(15)
                .dealerScore(10)
                .deckJson("[]")
                .playerCardsJson("[]")
                .dealerCardsJson("[]")
                .build();

        StepVerifier.create(gameRepository.save(game))
                .expectNextMatches(saved -> saved.getId() != null && saved.getPlayerId().equals("integrationTestPlayer"))
                .verifyComplete();

        StepVerifier.create(gameRepository.findByPlayerId("integrationTestPlayer"))
                .expectNextMatches(found -> found.getId() != null && found.getPlayerId().equals("integrationTestPlayer"))
                .verifyComplete();
    }
}


