package cat.itacademy.blackjack.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    private Long id;

    private String playerId; // Referencia a MongoDB
    private LocalDateTime createdAt;
    private GameStatus status;
    private int playerScore;
    private int dealerScore;
}