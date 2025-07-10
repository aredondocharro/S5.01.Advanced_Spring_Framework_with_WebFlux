package cat.itacademy.blackjack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;


@Configuration
@EnableReactiveMongoRepositories(basePackages = "cat.itacademy.blackjack.repository.mongo")
public class MongoConfig {

}
