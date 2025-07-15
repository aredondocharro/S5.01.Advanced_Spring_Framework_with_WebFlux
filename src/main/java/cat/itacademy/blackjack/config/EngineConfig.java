package cat.itacademy.blackjack.config;

import cat.itacademy.blackjack.service.engine.DeckManager;
import cat.itacademy.blackjack.service.engine.GameFactory;
import cat.itacademy.blackjack.service.engine.GameTurnEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    @Bean
    public DeckManager deckManager(ObjectMapper objectMapper) {
        return new DeckManager(objectMapper);
    }

    @Bean
    public GameFactory gameFactory(DeckManager deckManager) {
        return new GameFactory(deckManager);
    }

    @Bean
    public GameTurnEngine gameTurnEngine() {
        return new GameTurnEngine();
    }
}