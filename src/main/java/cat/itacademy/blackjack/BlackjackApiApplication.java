package cat.itacademy.blackjack;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@OpenAPIDefinition(
		info = @Info(
				title = "Blackjack API",
				version = "1.0",
				description = "Reactive Blackjack API with Spring WebFlux, MongoDB and MySQL"
		)
)
@SpringBootApplication
public class BlackjackApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(BlackjackApiApplication.class, args);
	}
}