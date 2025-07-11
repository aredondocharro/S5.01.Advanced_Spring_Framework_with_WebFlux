package cat.itacademy.blackjack;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;

public abstract class IntegrationTestBase {

    static MongoDBContainer mongoDB = new MongoDBContainer("mongo:7.0.5");
    static MySQLContainer<?> mySQL = new MySQLContainer<>("mysql:8.3.0")
            .withDatabaseName("blackjack")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void startContainers() {
        mongoDB.start();
        mySQL.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // MongoDB
        registry.add("spring.data.mongodb.uri", mongoDB::getReplicaSetUrl);

        // MySQL R2DBC
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:mysql://" + mySQL.getHost() + ":" + mySQL.getMappedPort(3306) + "/" + mySQL.getDatabaseName());
        registry.add("spring.r2dbc.username", mySQL::getUsername);
        registry.add("spring.r2dbc.password", mySQL::getPassword);
    }
}