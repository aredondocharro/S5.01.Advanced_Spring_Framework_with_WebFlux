package cat.itacademy.blackjack;

import org.springframework.boot.CommandLineRunner;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final DatabaseClient databaseClient;

    public DatabaseInitializer(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public void run(String... args) {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS games (
                id BIGSERIAL PRIMARY KEY,
                player_id VARCHAR(255) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                status VARCHAR(50) NOT NULL,
                turn VARCHAR(50) NOT NULL,
                player_score INT NOT NULL,
                dealer_score INT NOT NULL,
                deck_json TEXT NOT NULL,
                player_cards_json TEXT NOT NULL,
                dealer_cards_json TEXT NOT NULL
            );
            """;

        databaseClient.sql(createTableSql)
                .then()
                .doOnSuccess(unused -> System.out.println("✅ Tabla 'games' creada o ya existente."))
                .doOnError(error -> System.err.println("❌ Error creando tabla 'games': " + error.getMessage()))
                .subscribe();
    }
}