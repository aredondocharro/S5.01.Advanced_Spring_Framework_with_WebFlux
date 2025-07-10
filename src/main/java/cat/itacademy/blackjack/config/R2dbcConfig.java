package cat.itacademy.blackjack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "cat.itacademy.blackjack.repository.sql")
public class R2dbcConfig {

}
